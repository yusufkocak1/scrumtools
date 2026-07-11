package com.scrumtools.service.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * iyzico IYZWSv2 imza şeması (HMAC-SHA256).
 *
 * Şema (iyzico API v2 dokümantasyonu):
 *   payload        = randomKey + uriPath + requestBody
 *   signature      = hex( HMAC-SHA256(payload, secretKey) )
 *   authorization  = "apiKey:" + apiKey + "&randomKey:" + randomKey + "&signature:" + signature
 *   header         = "Authorization: IYZWSv2 " + base64(authorization)
 *
 * NOT: Entegrasyon canlıya alınmadan önce güncel iyzico dokümanıyla doğrulanmalı
 * (docs.iyzico.com → API v2 auth).
 */
public final class IyzicoAuthUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IyzicoAuthUtil() {}

    public static String generateRandomKey() {
        return System.currentTimeMillis() + String.valueOf(100000000 + RANDOM.nextInt(900000000));
    }

    /**
     * @param uriPath     query string dahil path (örn. "/v2/iyzilink/products")
     * @param requestBody GET/DELETE için boş string
     */
    public static String authorizationHeader(String apiKey, String secretKey,
                                             String randomKey, String uriPath, String requestBody) {
        String payload = randomKey + uriPath + (requestBody != null ? requestBody : "");
        String signature = hmacSha256Hex(payload, secretKey);
        String authorization = "apiKey:" + apiKey + "&randomKey:" + randomKey + "&signature:" + signature;
        return "IYZWSv2 " + Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
    }

    /** Webhook imza doğrulaması için de kullanılır. */
    public static String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 hesaplanamadı", e);
        }
    }
}
