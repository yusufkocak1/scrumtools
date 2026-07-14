package com.scrumtools.service.scm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScmBranchNamesTest {

    @Test
    void validNamesAccepted() {
        assertEquals("feature/SCRM-12-login-fix", ScmBranchNames.validate("feature/SCRM-12-login-fix"));
        assertEquals("bugfix/PAY-1", ScmBranchNames.validate("  bugfix/PAY-1  "));
        assertEquals("release/v1.2.3", ScmBranchNames.validate("release/v1.2.3"));
        assertEquals("hotfix_2024", ScmBranchNames.validate("hotfix_2024"));
    }

    @Test
    void blankRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate(null));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("   "));
    }

    @Test
    void invalidCharactersRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature/login fix"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("özellik/giriş"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feat~1"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feat:colon"));
    }

    @Test
    void gitRefRulesEnforced() {
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("/feature/x"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature/x/"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature//x"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature/..x"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("-feature"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate(".feature"));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature."));
        assertThrows(IllegalArgumentException.class, () -> ScmBranchNames.validate("feature.lock"));
    }

    @Test
    void overlongRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ScmBranchNames.validate("a".repeat(201)));
        assertEquals("a".repeat(200), ScmBranchNames.validate("a".repeat(200)));
    }
}
