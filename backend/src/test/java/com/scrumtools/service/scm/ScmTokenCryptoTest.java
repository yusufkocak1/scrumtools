package com.scrumtools.service.scm;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ScmTokenCryptoTest {

    private static String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void encryptDecryptRoundTrip() {
        ScmTokenCrypto crypto = new ScmTokenCrypto(randomKey());
        String token = "ghp_abc123XYZtokenDeğeriÜçNoktalı";

        String encrypted = crypto.encrypt(token);
        assertNotEquals(token, encrypted);
        assertEquals(token, crypto.decrypt(encrypted));
    }

    @Test
    void everyEncryptionUsesFreshIv() {
        ScmTokenCrypto crypto = new ScmTokenCrypto(randomKey());
        String token = "glpat-sameTokenTwice";

        assertNotEquals(crypto.encrypt(token), crypto.encrypt(token),
                "Aynı token iki şifrelemede farklı çıktı üretmeli (random IV)");
    }

    @Test
    void decryptWithWrongKeyFails() {
        ScmTokenCrypto cryptoA = new ScmTokenCrypto(randomKey());
        ScmTokenCrypto cryptoB = new ScmTokenCrypto(randomKey());
        String encrypted = cryptoA.encrypt("secret");

        assertThrows(IllegalStateException.class, () -> cryptoB.decrypt(encrypted));
    }

    @Test
    void corruptedCiphertextFails() {
        ScmTokenCrypto crypto = new ScmTokenCrypto(randomKey());
        assertThrows(IllegalStateException.class, () -> crypto.decrypt("bozuk-base64-degil!!"));
    }

    @Test
    void blankKeyDisablesCrypto() {
        ScmTokenCrypto crypto = new ScmTokenCrypto("");
        assertFalse(crypto.isEnabled());
        assertThrows(IllegalStateException.class, () -> crypto.encrypt("x"));
    }

    @Test
    void wrongKeyLengthRejected() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> new ScmTokenCrypto(shortKey));
    }

    @Test
    void hintMasksAllButLastFour() {
        ScmTokenCrypto crypto = new ScmTokenCrypto(randomKey());
        assertEquals("****abcd", crypto.hint("ghp_veryLongSecretTokenabcd"));
        assertEquals("****", crypto.hint("short"));
        assertEquals("****", crypto.hint(null));
    }
}
