package com.scrumtools.service.scm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SCM ve CI/CD token'larını AES-256-GCM ile şifreler/çözer.
 *
 * Anahtar env'den gelir: SCM_CRYPTO_KEY (base64, 32 byte). Boşsa SCM
 * entegrasyonu kapalı kabul edilir (iyzico "enabled" deseniyle aynı) —
 * bağlantı eklenmek istendiğinde anlaşılır hata dönülür.
 *
 * Saklama formatı: base64(iv || ciphertext+tag). Her şifrelemede random
 * 12 byte IV üretilir; aynı token iki kez şifrelenirse çıktılar farklıdır.
 */
@Component
public class ScmTokenCrypto {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public ScmTokenCrypto(@Value("${scm.crypto-key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            this.key = null;
            return;
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "SCM_CRYPTO_KEY 32 byte'lık base64 kodlu bir anahtar olmalı (mevcut: " + keyBytes.length + " byte).");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** Anahtar tanımlı mı — false ise SCM entegrasyonu devre dışıdır. */
    public boolean isEnabled() {
        return key != null;
    }

    public String encrypt(String plainText) {
        requireEnabled();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Token şifrelenemedi.", e);
        }
    }

    public String decrypt(String stored) {
        requireEnabled();
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Token çözülemedi — SCM_CRYPTO_KEY değişmiş olabilir.", e);
        }
    }

    /** Maskeli gösterim: son 4 karakter hariç gizlenir. */
    public String hint(String token) {
        if (token == null || token.length() < 8) return "****";
        return "****" + token.substring(token.length() - 4);
    }

    private void requireEnabled() {
        if (key == null) {
            throw new IllegalStateException(
                    "Entegrasyon şifreleme anahtarı bu sunucuda yapılandırılmamış (SCM_CRYPTO_KEY tanımlı değil).");
        }
    }
}
