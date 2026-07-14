package com.scrumtools.service.scm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScmUrlValidatorTest {

    private final ScmUrlValidator validator = new ScmUrlValidator(false);
    private final ScmUrlValidator permissive = new ScmUrlValidator(true);

    @Test
    void nullAndBlankAreValid() {
        assertDoesNotThrow(() -> validator.validate(null));
        assertDoesNotThrow(() -> validator.validate("  "));
    }

    @Test
    void publicHttpsUrlAccepted() {
        assertDoesNotThrow(() -> validator.validate("https://gitlab.com"));
    }

    @Test
    void urlWithPathAccepted() {
        // Reverse proxy arkasındaki self-hosted kurulumlar: https://git.firma.com/gitlab
        assertDoesNotThrow(() -> permissive.validate("https://git.firma.com/gitlab"));
    }

    @Test
    void nonHttpSchemeRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("ftp://example.com"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("file:///etc/passwd"));
    }

    @Test
    void userInfoRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://admin@evil.com"));
    }

    @Test
    void fragmentRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://example.com#fragment"));
    }

    @Test
    void loopbackRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://127.0.0.1:8080"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://localhost"));
    }

    @Test
    void privateRangesRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://10.0.0.5"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://172.16.1.1"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://192.168.1.10"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://169.254.1.1"));
    }

    @Test
    void privateHostsAllowedWithFlag() {
        assertDoesNotThrow(() -> permissive.validate("http://192.168.1.10:3000"));
        assertDoesNotThrow(() -> permissive.validate("http://10.0.0.5/gitea"));
    }
}
