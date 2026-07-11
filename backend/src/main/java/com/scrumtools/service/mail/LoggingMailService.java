package com.scrumtools.service.mail;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SMTP yapılandırılmamışken (app.mail.enabled=false / tanımsız) devreye giren fallback.
 * Gönderilecek maili log'a yazar — dev/test ortamında tüm akışlar mail sunucusu
 * olmadan çalışmaya devam eder.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailService implements MailService {

    @Override
    public void sendMemberInvite(User user, String orgName, String setupUrl) {
        log.info("[MAIL devre dışı] Üye daveti → {} | org: {} | setupUrl: {}", user.getEmail(), orgName, setupUrl);
    }

    @Override
    public void sendPasswordReset(User user, String resetUrl) {
        log.info("[MAIL devre dışı] Şifre sıfırlama → {} | resetUrl: {}", user.getEmail(), resetUrl);
    }

    @Override
    public void sendTrialExpiring(Organization org, long daysLeft) {
        log.info("[MAIL devre dışı] Trial bitiyor ({} gün) → {} | org: {}", daysLeft, org.getOwner().getEmail(), org.getName());
    }

    @Override
    public void sendTrialExpired(Organization org) {
        log.info("[MAIL devre dışı] Trial bitti → {} | org: {}", org.getOwner().getEmail(), org.getName());
    }

    @Override
    public void sendPaymentLink(Organization org, String planName, String cycleLabel, String paymentUrl) {
        log.info("[MAIL devre dışı] Ödeme linki → {} | plan: {} ({}) | url: {}",
                org.getOwner().getEmail(), planName, cycleLabel, paymentUrl);
    }

    @Override
    public void sendPaymentReceived(Organization org, String planName, LocalDateTime periodEnd) {
        log.info("[MAIL devre dışı] Ödeme alındı → {} | plan: {} | dönem sonu: {}",
                org.getOwner().getEmail(), planName, periodEnd);
    }

    @Override
    public void sendSubscriptionExpiring(Organization org, long daysLeft, String renewUrl) {
        log.info("[MAIL devre dışı] Abonelik bitiyor ({} gün) → {} | org: {} | renewUrl: {}",
                daysLeft, org.getOwner().getEmail(), org.getName(), renewUrl);
    }

    @Override
    public void sendSubscriptionExpired(Organization org) {
        log.info("[MAIL devre dışı] Abonelik bitti → {} | org: {}", org.getOwner().getEmail(), org.getName());
    }
}
