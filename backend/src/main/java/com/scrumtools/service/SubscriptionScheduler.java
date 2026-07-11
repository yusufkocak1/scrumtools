package com.scrumtools.service;

import com.scrumtools.entity.PaymentTransaction;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.enums.PaymentStatus;
import com.scrumtools.entity.enums.SubscriptionStatus;
import com.scrumtools.repository.PaymentTransactionRepository;
import com.scrumtools.repository.SubscriptionRepository;
import com.scrumtools.service.mail.MailService;
import com.scrumtools.service.payment.PaymentProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Abonelik yaşam döngüsü zamanlayıcıları.
 *
 * NOT: Kod LocalDateTime kullanır — container'ın TZ=Europe/Istanbul ile
 * çalıştığından emin olun; cron'lar da aynı zone'a sabitlenmiştir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    /** Dönem bittikten sonra entitlement'ların devam ettiği ek süre (gün). */
    private static final int GRACE_DAYS = 3;

    /** Hatırlatma günleri → remindersSent bitmask bitleri. */
    private static final int[] REMINDER_DAYS = {7, 3, 1};

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionService subscriptionService;
    private final BillingService billingService;
    private final MailService mailService;
    private final PaymentProviderService paymentProvider;

    // ─── 1. Süre dolumu taraması (saat başı) ─────────────────────────────────

    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Istanbul")
    @Transactional
    public void expirySweep() {
        LocalDateTime now = LocalDateTime.now();

        // Trial süresi dolanlar → EXPIRED + FREE'ye düşür
        for (Subscription sub : subscriptionRepository
                .findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, now)) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            subscriptionService.downgradeToFree(sub.getOrganization());
            mailService.sendTrialExpired(sub.getOrganization());
            log.info("Trial süresi doldu: org='{}'", sub.getOrganization().getSlug());
        }

        // Dönemi dolan aktif abonelikler → PAST_DUE (grace period başlar)
        for (Subscription sub : subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, now)) {
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(sub);
            log.info("Abonelik PAST_DUE'ya geçti (grace {} gün): org='{}'",
                    GRACE_DAYS, sub.getOrganization().getSlug());
        }

        // Grace süresi de dolanlar → EXPIRED + FREE'ye düşür
        for (Subscription sub : subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.PAST_DUE, now.minusDays(GRACE_DAYS))) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            subscriptionService.downgradeToFree(sub.getOrganization());
            mailService.sendSubscriptionExpired(sub.getOrganization());
            log.info("Abonelik süresi doldu: org='{}'", sub.getOrganization().getSlug());
        }
    }

    // ─── 2. Yenileme hatırlatmaları (her gün 09:00) ──────────────────────────

    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void renewalReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> live = subscriptionRepository.findByStatusIn(
                List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE));

        for (Subscription sub : live) {
            LocalDateTime end = sub.getStatus() == SubscriptionStatus.TRIAL
                    ? sub.getTrialEndsAt() : sub.getCurrentPeriodEnd();
            if (end == null || end.isBefore(now)) continue;

            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate().atStartOfDay(),
                    end.toLocalDate().atStartOfDay());

            for (int i = 0; i < REMINDER_DAYS.length; i++) {
                int bit = 1 << i;
                if (daysLeft == REMINDER_DAYS[i] && (sub.getRemindersSent() & bit) == 0) {
                    sendReminder(sub, daysLeft);
                    sub.setRemindersSent(sub.getRemindersSent() | bit);
                    subscriptionRepository.save(sub);
                    break;
                }
            }
        }
    }

    private void sendReminder(Subscription sub, long daysLeft) {
        if (sub.getStatus() == SubscriptionStatus.TRIAL) {
            mailService.sendTrialExpiring(sub.getOrganization(), daysLeft);
        } else {
            // Yenileme maili — iyzilink yoksa (renewUrl boş) mail Abonelik sekmesine yönlendirir
            mailService.sendSubscriptionExpiring(sub.getOrganization(), daysLeft, null);
        }
        log.info("Hatırlatma gönderildi ({} gün): org='{}' status={}",
                daysLeft, sub.getOrganization().getSlug(), sub.getStatus());
    }

    // ─── 3. Ödeme reconciliation poller (15 dk'da bir) ───────────────────────
    // Kaçan webhook'ları yakalar; lokal geliştirmede public URL olmadan da
    // uçtan uca ödeme akışını tamamlar.

    @Scheduled(fixedDelay = 900_000, initialDelay = 60_000)
    public void reconcilePendingPayments() {
        if (!paymentProvider.isEnabled()) return;

        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> pending = paymentTransactionRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, now.minusMinutes(5));

        for (PaymentTransaction tx : pending) {
            try {
                if (tx.getCreatedAt().isBefore(now.minusDays(7))) {
                    // 7 günden eski link — süresi doldu say, linki kapat
                    tx.setStatus(PaymentStatus.EXPIRED);
                    paymentTransactionRepository.save(tx);
                    if (tx.getIyzilinkToken() != null) paymentProvider.disableLink(tx.getIyzilinkToken());
                    continue;
                }
                if (tx.getIyzilinkToken() != null) {
                    // verifyAndActivate kendi transaction'ında satır kilidiyle çalışır (webhook ile yarışabilir)
                    if (billingService.verifyAndActivate(tx.getId(), null)) {
                        log.info("Reconciliation ile ödeme yakalandı: tx={}", tx.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Reconciliation hatası (tx={}): {}", tx.getId(), e.getMessage());
            }
        }
    }
}
