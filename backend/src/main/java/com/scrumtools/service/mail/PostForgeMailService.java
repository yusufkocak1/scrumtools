package com.scrumtools.service.mail;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PostForge (https://postforge.kocak.net.tr) üzerinden e-posta gönderimi
 * (app.mail.provider=postforge iken aktif).
 * <p>
 * Konu ve HTML gövde PostForge'daki şablonlarda tutulur; buradan yalnızca
 * {@code templateCode} + {@code params} gönderilir. Şablon kaynakları ve
 * import script'i repo kökündeki {@code postforge/} klasöründedir.
 * <p>
 * Tüm metodlar @Async — istek thread'ini bloklamaz; hatalar loglanır, fırlatılmaz.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "postforge")
public class PostForgeMailService implements MailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"));

    // PostForge'daki şablon kodları (postforge/templates.json ile birebir aynı olmalı)
    private static final String T_MEMBER_INVITE = "scrumtools-member-invite";
    private static final String T_PASSWORD_RESET = "scrumtools-password-reset";
    private static final String T_TRIAL_EXPIRING = "scrumtools-trial-expiring";
    private static final String T_TRIAL_EXPIRED = "scrumtools-trial-expired";
    private static final String T_PAYMENT_LINK = "scrumtools-payment-link";
    private static final String T_PAYMENT_RECEIVED = "scrumtools-payment-received";
    private static final String T_SUBSCRIPTION_EXPIRING = "scrumtools-subscription-expiring";
    private static final String T_SUBSCRIPTION_EXPIRED = "scrumtools-subscription-expired";

    private final RestClient restClient;

    /** Boşsa PostForge organizasyonundaki varsayılan gönderici hesabı kullanılır. */
    private final String senderCode;

    public PostForgeMailService(@Value("${app.mail.postforge.base-url}") String baseUrl,
                                @Value("${app.mail.postforge.api-key}") String apiKey,
                                @Value("${app.mail.postforge.sender-code:}") String senderCode,
                                @Value("${app.mail.postforge.timeout-seconds:10}") int timeoutSeconds) {
        this.senderCode = senderCode == null || senderCode.isBlank() ? null : senderCode;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .requestFactory(factory)
                .defaultHeader("X-Api-Key", apiKey)
                .build();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("PostForge API anahtarı boş — mail gönderimleri 401 ile başarısız olacak "
                    + "(POSTFORGE_API_KEY tanımlayın).");
        }
    }

    @Async
    @Override
    public void sendMemberInvite(User user, String orgName, String setupUrl) {
        send(T_MEMBER_INVITE, user.getEmail(), params(
                "name", user.getName(), "orgName", orgName, "setupUrl", setupUrl));
    }

    @Async
    @Override
    public void sendPasswordReset(User user, String resetUrl) {
        send(T_PASSWORD_RESET, user.getEmail(), params(
                "name", user.getName(), "resetUrl", resetUrl));
    }

    @Async
    @Override
    public void sendTrialExpiring(Organization org, long daysLeft) {
        send(T_TRIAL_EXPIRING, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName(), "daysLeft", daysLeft));
    }

    @Async
    @Override
    public void sendTrialExpired(Organization org) {
        send(T_TRIAL_EXPIRED, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName()));
    }

    @Async
    @Override
    public void sendPaymentLink(Organization org, String planName, String cycleLabel, String paymentUrl) {
        send(T_PAYMENT_LINK, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName(),
                "planName", planName, "cycleLabel", cycleLabel, "paymentUrl", paymentUrl));
    }

    @Async
    @Override
    public void sendPaymentReceived(Organization org, String planName, LocalDateTime periodEnd) {
        send(T_PAYMENT_RECEIVED, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName(), "planName", planName,
                "periodEnd", periodEnd != null ? periodEnd.format(DATE_FMT) : "-"));
    }

    @Async
    @Override
    public void sendSubscriptionExpiring(Organization org, long daysLeft, String renewUrl) {
        boolean hasRenewUrl = renewUrl != null && !renewUrl.isBlank();
        // hasRenewUrl bilerek boolean: Mustache'ta boş string truthy sayılır,
        // {{#hasRenewUrl}} bölümü yalnızca gerçek bir link varken render edilmeli.
        send(T_SUBSCRIPTION_EXPIRING, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName(), "daysLeft", daysLeft,
                "hasRenewUrl", hasRenewUrl, "renewUrl", hasRenewUrl ? renewUrl : ""));
    }

    @Async
    @Override
    public void sendSubscriptionExpired(Organization org) {
        send(T_SUBSCRIPTION_EXPIRED, ownerEmail(org), params(
                "name", ownerName(org), "orgName", org.getName()));
    }

    private void send(String templateCode, String to, Map<String, Object> params) {
        if (to == null || to.isBlank()) {
            log.warn("PostForge gönderimi atlandı ({}): alıcı adresi yok", templateCode);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("templateCode", templateCode);
            body.put("to", to);
            body.put("params", params);
            if (senderCode != null) {
                body.put("senderCode", senderCode);
            }

            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("PostForge e-posta kuyruğa alındı: {} → {} (messageId={})",
                    templateCode, to, response == null ? null : response.get("messageId"));
        } catch (RestClientResponseException e) {
            // Mail hatası iş akışını bozmamalı — logla ve devam et. API key loglanmaz.
            log.error("PostForge e-posta gönderilemedi ({} → {}): HTTP {} {}",
                    templateCode, to, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("PostForge e-posta gönderilemedi ({} → {}): {}", templateCode, to, e.getMessage());
        }
    }

    /** Map.of null kabul etmediği için null değerleri boş stringe çeviren yardımcı. */
    private Map<String, Object> params(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            map.put(String.valueOf(keyValues[i]), value == null ? "" : value);
        }
        return map;
    }

    private String ownerEmail(Organization org) {
        return org.getOwner() == null ? null : org.getOwner().getEmail();
    }

    private String ownerName(Organization org) {
        return org.getOwner() == null ? "" : org.getOwner().getName();
    }
}
