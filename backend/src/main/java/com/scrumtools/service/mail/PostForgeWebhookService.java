package com.scrumtools.service.mail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * PostForge bildirimlerini (message.sent/failed/opened/clicked) işler.
 * <p>
 * İmza: {@code X-PostForge-Signature: t=<zaman>,v1=<hex>} — v1, webhook gizli
 * anahtarıyla {@code "<t>.<ham gövde>"} metninin HMAC-SHA256 özetidir. Doğrulama HAM
 * gövde üzerinden yapılmak zorundadır; parse edip yeniden serialize etmek imzayı bozar.
 * <p>
 * Tekrarlar {@code X-PostForge-Delivery} kimliğiyle yutulur — aynı olay yeniden
 * denenebildiği için sayaçlar aksi halde şişerdi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostForgeWebhookService {

    /** İmzadaki zaman damgası için kabul edilen sapma — replay saldırısına karşı. */
    private static final long MAX_CLOCK_SKEW_SECONDS = 300;

    private final EmailLogService emailLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.postforge.webhook-secret:}")
    private String webhookSecret;

    /** İmza geçersizse false döner — controller 401 çevirir (PostForge tekrar dener). */
    public boolean handle(String rawBody, String signatureHeader, String deliveryId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("PostForge webhook: POSTFORGE_WEBHOOK_SECRET tanımlı değil, istek reddedildi");
            return false;
        }

        Signature signature = Signature.parse(signatureHeader);
        if (signature == null || !signature.verify(webhookSecret, rawBody)) {
            log.warn("PostForge webhook: imza doğrulanamadı (delivery={})", deliveryId);
            return false;
        }
        if (signature.isStale()) {
            log.warn("PostForge webhook: imza zaman damgası eski, replay olabilir (delivery={})", deliveryId);
            return false;
        }

        // İmza geçerli — bundan sonraki hatalar 200 ile yutulur, tekrar denenmesi fayda etmez
        try {
            process(rawBody, deliveryId);
        } catch (Exception e) {
            log.error("PostForge webhook işlenemedi (delivery={}): {}", deliveryId, e.getMessage());
        }
        return true;
    }

    private void process(String rawBody, String deliveryId) throws Exception {
        Map<String, Object> payload =
                objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});

        String event = str(payload.get("event"));
        Map<String, Object> data = asMap(payload.get("data"));
        String messageId = str(data.get("messageId"));

        if (event == null || messageId == null) {
            log.warn("PostForge webhook: event/messageId yok, atlandı");
            return;
        }

        emailLogService.applyEvent(
                event,
                messageId,
                str(data.get("to")),
                str(data.get("templateCode")),
                str(data.get("error")) != null ? str(data.get("error")) : str(data.get("status")),
                LocalDateTime.now(),
                deliveryId);
    }

    // ─── İmza ─────────────────────────────────────────────────────────────────

    /** {@code t=<zaman>,v1=<hex>} başlığının ayrıştırılmış hali (test için package-private). */
    record Signature(String timestamp, String v1) {

        static Signature parse(String header) {
            if (header == null || header.isBlank()) return null;
            String t = null, v1 = null;
            for (String part : header.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length != 2) continue;
                if ("t".equals(kv[0])) t = kv[1].trim();
                else if ("v1".equals(kv[0])) v1 = kv[1].trim();
            }
            return (t == null || v1 == null) ? null : new Signature(t, v1);
        }

        boolean verify(String secret, String rawBody) {
            String expected = hmacSha256Hex(secret, timestamp + "." + rawBody);
            return MessageDigest.isEqual(
                    v1.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        }

        /** Zaman damgası saniye veya milisaniye gelebilir; büyüklüğünden ayırt edilir. */
        boolean isStale() {
            try {
                long value = Long.parseLong(timestamp);
                long epochSeconds = value > 1_000_000_000_000L ? value / 1000 : value;
                return Math.abs(Instant.now().getEpochSecond() - epochSeconds) > MAX_CLOCK_SKEW_SECONDS;
            } catch (NumberFormatException e) {
                // Beklenmeyen format — imza zaten doğrulandı, tazelik kontrolünü atla
                return false;
            }
        }

        private static String hmacSha256Hex(String secret, String payload) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new IllegalStateException("HMAC hesaplanamadı", e);
            }
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }
}
