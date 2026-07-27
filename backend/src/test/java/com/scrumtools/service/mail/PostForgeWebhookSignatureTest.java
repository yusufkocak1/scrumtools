package com.scrumtools.service.mail;

import com.scrumtools.service.mail.PostForgeWebhookService.Signature;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostForge imzası: {@code t=<zaman>,v1=<hex>}; v1, "<t>.<ham gövde>" metninin
 * webhook gizli anahtarıyla HMAC-SHA256 özetidir.
 */
class PostForgeWebhookSignatureTest {

    private static final String SECRET = "pf-webhook-s3cr3t";
    private static final String BODY =
            "{\"event\":\"message.sent\",\"data\":{\"messageId\":\"msg_1\"}}";

    private static String hmac(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String header(String secret, String timestamp, String body) throws Exception {
        return "t=" + timestamp + ",v1=" + hmac(secret, timestamp + "." + body);
    }

    private static String now() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    @Test
    void gecerliImzaKabulEdilir() throws Exception {
        Signature signature = Signature.parse(header(SECRET, now(), BODY));
        assertNotNull(signature);
        assertTrue(signature.verify(SECRET, BODY));
    }

    @Test
    void yanlisAnahtarReddedilir() throws Exception {
        Signature signature = Signature.parse(header("baska-secret", now(), BODY));
        assertFalse(signature.verify(SECRET, BODY));
    }

    @Test
    void govdeDegistirilirseReddedilir() throws Exception {
        Signature signature = Signature.parse(header(SECRET, now(), BODY));
        assertFalse(signature.verify(SECRET, BODY.replace("msg_1", "msg_2")));
    }

    /** İmza zaman damgası gövdeye dahil — t değiştirilirse imza tutmaz. */
    @Test
    void zamanDamgasiDegistirilirseReddedilir() throws Exception {
        String original = now();
        Signature tampered = Signature.parse(
                header(SECRET, original, BODY).replace("t=" + original, "t=" + (Long.parseLong(original) + 60)));
        assertFalse(tampered.verify(SECRET, BODY));
    }

    @Test
    void bozukBaslikNullDoner() {
        assertNull(Signature.parse(null));
        assertNull(Signature.parse(""));
        assertNull(Signature.parse("v1=abc"));          // t yok
        assertNull(Signature.parse("t=123"));            // v1 yok
        assertNull(Signature.parse("gecersiz-baslik"));
    }

    @Test
    void guncelZamanDamgasiTazeSayilir() throws Exception {
        assertFalse(Signature.parse(header(SECRET, now(), BODY)).isStale());
    }

    @Test
    void eskiZamanDamgasiReplayOlarakReddedilir() throws Exception {
        String old = String.valueOf(Instant.now().getEpochSecond() - 3600);
        assertTrue(Signature.parse(header(SECRET, old, BODY)).isStale());
    }

    /** PostForge saniye yerine milisaniye gönderirse de tazelik doğru hesaplanmalı. */
    @Test
    void milisaniyeZamanDamgasiDesteklenir() throws Exception {
        String millis = String.valueOf(Instant.now().toEpochMilli());
        assertFalse(Signature.parse(header(SECRET, millis, BODY)).isStale());
    }
}
