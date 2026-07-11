package com.scrumtools.service;

import com.scrumtools.dto.CheckoutResponse;
import com.scrumtools.dto.PaymentTransactionResponse;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.PaymentTransaction;
import com.scrumtools.entity.Plan;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.PaymentStatus;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.PaymentTransactionRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.mail.MailService;
import com.scrumtools.service.payment.PaymentProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ödeme linki oluşturma + ödemenin doğrulanıp aboneliğe dönüşmesi.
 * Aktivasyonun kendisi SubscriptionService.activate'te — webhook, poller ve
 * superadmin manuel yolu aynı kodu paylaşır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final PaymentProviderService paymentProvider;
    private final MailService mailService;

    /** Müşteri (org owner/admin) satın alma başlatır → iyzilink URL döner. */
    @Transactional
    public CheckoutResponse checkout(UUID orgId, String requesterEmail, String planCode, BillingCycle cycle) {
        checkAdminAccess(orgId, requesterEmail);
        User requester = getUser(requesterEmail);
        PaymentTransaction tx = createLinkTransaction(orgId, planCode, cycle, requester);
        return new CheckoutResponse(tx.getId(), tx.getIyzilinkUrl());
    }

    /** Superadmin: ödeme linki oluştur + org sahibine e-posta ile gönder. */
    @Transactional
    public PaymentTransactionResponse adminCreatePaymentLink(UUID orgId, String planCode,
                                                             BillingCycle cycle, User actor) {
        PaymentTransaction tx = createLinkTransaction(orgId, planCode, cycle, actor);
        mailService.sendPaymentLink(tx.getOrganization(), tx.getPlan().getName(),
                cycleLabel(cycle), tx.getIyzilinkUrl());
        return toResponse(tx);
    }

    /** Org owner/admin ödeme geçmişi. */
    public List<PaymentTransactionResponse> getTransactions(UUID orgId, String requesterEmail) {
        checkAdminAccess(orgId, requesterEmail);
        return getTransactionsInternal(orgId);
    }

    /** Superadmin ödeme geçmişi (yetki kontrolü controller'daki @PreAuthorize'da). */
    public List<PaymentTransactionResponse> getTransactionsInternal(UUID orgId) {
        return paymentTransactionRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Webhook/poller: sağlayıcıdan durumu server-to-server teyit et; ödendiyse aktive et.
     * Webhook payload'ına asla tek başına güvenilmez.
     *
     * @return ödeme doğrulanıp işlendiyse true
     */
    @Transactional
    public boolean verifyAndActivate(UUID transactionId, String rawWebhookPayload) {
        PaymentTransaction tx = paymentTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme kaydı bulunamadı: " + transactionId));

        if (tx.getStatus() == PaymentStatus.PAID) return true; // idempotent

        PaymentProviderService.LinkStatus status = paymentProvider.getLinkStatus(tx.getIyzilinkToken());
        if (!status.paid()) return false;

        tx.setStatus(PaymentStatus.PAID);
        tx.setPaidAt(LocalDateTime.now());
        tx.setProviderPaymentId(status.providerPaymentId());
        if (rawWebhookPayload != null) tx.setRawWebhookPayload(rawWebhookPayload);

        Subscription sub = subscriptionService.activate(
                tx.getOrganization().getId(),
                tx.getPlan().getCode(),
                tx.getBillingCycle(),
                SubscriptionSource.IYZILINK,
                tx.getInitiatedBy(),
                null);
        tx.setSubscription(sub);
        paymentTransactionRepository.save(tx);
        log.info("Ödeme doğrulandı ve abonelik aktive edildi: tx={} org='{}'",
                tx.getId(), tx.getOrganization().getSlug());
        return true;
    }

    // ─── İç yardımcılar ───────────────────────────────────────────────────────

    private PaymentTransaction createLinkTransaction(UUID orgId, String planCode,
                                                     BillingCycle cycle, User initiator) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
        Plan plan = planService.getByCode(planCode);
        if (Boolean.TRUE.equals(plan.getIsDefault())) {
            throw new IllegalArgumentException("Ücretsiz plan satın alınamaz.");
        }
        BigDecimal price = cycle == BillingCycle.YEARLY ? plan.getYearlyPriceTry() : plan.getMonthlyPriceTry();
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Bu plan için " + cycleLabel(cycle) + " fiyat tanımlı değil.");
        }

        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .organization(org)
                .plan(plan)
                .billingCycle(cycle)
                .amount(price)
                .provider("IYZILINK")
                .initiatedBy(initiator)
                .build());
        tx.setConversationId(tx.getId().toString());

        PaymentProviderService.PaymentLink link = paymentProvider.createPaymentLink(
                new PaymentProviderService.CreateLinkCommand(
                        "ScrumTools " + plan.getName() + " (" + cycleLabel(cycle) + ")",
                        org.getName() + " organizasyonu için ScrumTools aboneliği (KDV dahil)",
                        price,
                        tx.getConversationId()));
        tx.setIyzilinkToken(link.token());
        tx.setIyzilinkUrl(link.url());
        return paymentTransactionRepository.save(tx);
    }

    private void checkAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Bu işlem için yeterli yetkiniz yok.");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    public static String cycleLabel(BillingCycle cycle) {
        return cycle == BillingCycle.YEARLY ? "yıllık" : "aylık";
    }

    public PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return new PaymentTransactionResponse(
                tx.getId(),
                tx.getPlan().getCode(),
                tx.getPlan().getName(),
                tx.getBillingCycle(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getStatus() == PaymentStatus.PENDING ? tx.getIyzilinkUrl() : null,
                tx.getPaidAt(),
                tx.getCreatedAt()
        );
    }
}
