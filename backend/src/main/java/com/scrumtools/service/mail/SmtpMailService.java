package com.scrumtools.service.mail;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * SMTP üzerinden gerçek e-posta gönderimi (app.mail.provider=smtp iken aktif).
 * Şablonlar: resources/templates/mail/*.html (Thymeleaf).
 * Tüm metodlar @Async — istek thread'ini bloklamaz; hatalar loglanır, fırlatılmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpMailService implements MailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"));

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String from;

    @Async
    @Override
    public void sendMemberInvite(User user, String orgName, String setupUrl) {
        send(user.getEmail(), orgName + " ekibine davet edildiniz",
                "mail/member-invite",
                Map.of("name", user.getName(), "orgName", orgName, "setupUrl", setupUrl));
    }

    @Async
    @Override
    public void sendPasswordReset(User user, String resetUrl) {
        send(user.getEmail(), "Şifre sıfırlama talebiniz",
                "mail/password-reset",
                Map.of("name", user.getName(), "resetUrl", resetUrl));
    }

    @Async
    @Override
    public void sendTrialExpiring(Organization org, long daysLeft) {
        send(org.getOwner().getEmail(), "Deneme süreniz " + daysLeft + " gün sonra bitiyor",
                "mail/trial-expiring",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName(), "daysLeft", daysLeft));
    }

    @Async
    @Override
    public void sendTrialExpired(Organization org) {
        send(org.getOwner().getEmail(), "Deneme süreniz sona erdi",
                "mail/trial-expired",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName()));
    }

    @Async
    @Override
    public void sendPaymentLink(Organization org, String planName, String cycleLabel, String paymentUrl) {
        send(org.getOwner().getEmail(), "ScrumTools " + planName + " paketi ödeme linkiniz",
                "mail/payment-link",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName(),
                        "planName", planName, "cycleLabel", cycleLabel, "paymentUrl", paymentUrl));
    }

    @Async
    @Override
    public void sendPaymentReceived(Organization org, String planName, LocalDateTime periodEnd) {
        send(org.getOwner().getEmail(), "Ödemeniz alındı — " + planName + " paketi aktif",
                "mail/payment-received",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName(),
                        "planName", planName,
                        "periodEnd", periodEnd != null ? periodEnd.format(DATE_FMT) : "-"));
    }

    @Async
    @Override
    public void sendSubscriptionExpiring(Organization org, long daysLeft, String renewUrl) {
        send(org.getOwner().getEmail(), "Aboneliğiniz " + daysLeft + " gün sonra sona eriyor",
                "mail/subscription-expiring",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName(),
                        "daysLeft", daysLeft, "renewUrl", renewUrl != null ? renewUrl : ""));
    }

    @Async
    @Override
    public void sendSubscriptionExpired(Organization org) {
        send(org.getOwner().getEmail(), "Aboneliğiniz sona erdi",
                "mail/subscription-expired",
                Map.of("name", org.getOwner().getName(), "orgName", org.getName()));
    }

    private void send(String to, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context(Locale.forLanguageTag("tr"));
            context.setVariables(variables);
            String html = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("E-posta gönderildi: {} → {}", template, to);
        } catch (Exception e) {
            // Mail hatası iş akışını bozmamalı — logla ve devam et
            log.error("E-posta gönderilemedi ({} → {}): {}", template, to, e.getMessage());
        }
    }
}
