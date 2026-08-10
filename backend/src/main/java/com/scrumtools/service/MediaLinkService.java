package com.scrumtools.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * İçeriğe gömülen medya (açıklama/yorum/doc sayfalarındaki görseller) için kalıcı,
 * imzalı bağlantı üretir.
 *
 * MinIO presigned URL'leri 60 dakikada geçersizleşir; içeriğin HTML'ine gömüldüklerinde
 * bir süre sonra görseller kırılıyordu. Buradaki URL ise ek kaydının kimliğine bağlıdır,
 * süresi dolmaz ve içerikle birlikte güvenle saklanabilir.
 *
 * Yetkilendirme imzanın kendisidir: /api/media/** oturum istemez (tarayıcı &lt;img&gt;
 * etiketleri Authorization header'ı gönderemez), bunun yerine URL'deki HMAC-SHA256
 * imzası doğrulanır — bağlantıyı bilen dosyayı görebilir.
 */
@Service
public class MediaLinkService {

    /** Task ekleri — /api/media/task-attachments/{id} */
    public static final String TASK_ATTACHMENT = "task-attachments";

    /** Doküman sayfası ekleri — /api/media/doc-attachments/{id} */
    public static final String DOC_ATTACHMENT = "doc-attachments";

    /** Quiz sorusu görselleri — /api/media/quiz-images/{id} */
    public static final String QUIZ_IMAGE = "quiz-images";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** İmzanın URL'de taşınan uzunluğu — 22 base64 karakteri ≈ 132 bit, tahmin edilemez. */
    private static final int SIGNATURE_LENGTH = 22;

    private final byte[] secret;

    public MediaLinkService(@Value("${app.media.secret:${app.jwt.secret}}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * İçeriğe gömülecek kalıcı URL — uygulama köküne göreli (ortamdan bağımsız olsun diye
     * host bilgisi içermez).
     */
    public String urlFor(String type, UUID id) {
        return "/api/media/" + type + "/" + id + "?s=" + sign(type, id);
    }

    /** URL'deki imzayı doğrular (zamanlama saldırısına kapalı karşılaştırma). */
    public boolean verify(String type, UUID id, String signature) {
        if (signature == null || signature.isBlank()) return false;
        return MessageDigest.isEqual(
                sign(type, id).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String type, UUID id) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((type + ":" + id).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest)
                    .substring(0, SIGNATURE_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Medya imzası üretilemedi: " + e.getMessage(), e);
        }
    }
}
