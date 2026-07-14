package com.scrumtools.service.scm;

import com.scrumtools.entity.enums.ScmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScmWebhookSignatureTest {

    private static final String SECRET = "s3cr3t-webhook-key";
    private static final String BODY = "{\"ref\":\"refs/heads/main\",\"commits\":[]}";

    private static String hmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void gitHubValidSignatureAccepted() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hub-Signature-256", "sha256=" + hmac(SECRET, BODY));
        assertTrue(ScmWebhookService.verifySignature(ScmProvider.GITHUB, SECRET, BODY, headers));
    }

    @Test
    void gitHubInvalidSignatureRejected() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hub-Signature-256", "sha256=" + hmac("yanlis-secret", BODY));
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITHUB, SECRET, BODY, headers));
    }

    @Test
    void gitHubTamperedBodyRejected() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hub-Signature-256", "sha256=" + hmac(SECRET, BODY));
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITHUB, SECRET,
                BODY + "tampered", headers));
    }

    @Test
    void missingHeaderRejected() {
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITHUB, SECRET, BODY, new HttpHeaders()));
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITLAB, SECRET, BODY, new HttpHeaders()));
    }

    @Test
    void gitLabPlainTokenComparison() {
        HttpHeaders valid = new HttpHeaders();
        valid.set("X-Gitlab-Token", SECRET);
        assertTrue(ScmWebhookService.verifySignature(ScmProvider.GITLAB, SECRET, BODY, valid));

        HttpHeaders invalid = new HttpHeaders();
        invalid.set("X-Gitlab-Token", "yanlis");
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITLAB, SECRET, BODY, invalid));
    }

    @Test
    void giteaHmacWithoutPrefix() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitea-Signature", hmac(SECRET, BODY));
        assertTrue(ScmWebhookService.verifySignature(ScmProvider.GITEA, SECRET, BODY, headers));
    }

    @Test
    void blankSecretAlwaysRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Token", "");
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITLAB, "", BODY, headers));
        assertFalse(ScmWebhookService.verifySignature(ScmProvider.GITHUB, null, BODY, headers));
    }
}
