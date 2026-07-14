package com.scrumtools.service.scm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScmCommitLinkerTest {

    @Test
    void extractsSingleKey() {
        assertEquals(Set.of("SCRM-12"),
                ScmCommitLinker.extractTaskKeys("SCRM-12 login hatası düzeltildi"));
    }

    @Test
    void extractsMultipleKeys() {
        Set<String> keys = ScmCommitLinker.extractTaskKeys("[SCRM-12][SCRM-13] ortak düzeltme PAY-4 ile");
        assertEquals(List.of("SCRM-12", "SCRM-13", "PAY-4"), List.copyOf(keys));
    }

    @Test
    void duplicateKeysReturnedOnce() {
        assertEquals(Set.of("SCRM-12"),
                ScmCommitLinker.extractTaskKeys("SCRM-12 fix, SCRM-12 tekrar"));
    }

    @Test
    void wordBoundaryEnforced() {
        // "XSCRM-12" içinden SCRM-12 sızmamalı — tamamı (XSCRM-12) ayrı bir anahtar
        // adayı olarak çıkar, takım çözümlemesinde elenir
        Set<String> keys = ScmCommitLinker.extractTaskKeys("XSCRM-12 fix");
        assertFalse(keys.contains("SCRM-12"));
        assertEquals(Set.of("XSCRM-12"), keys);
        // Branch adı içinde tire ile devam etse de anahtar yakalanır
        assertEquals(Set.of("SCRM-12"), ScmCommitLinker.extractTaskKeys("feature/SCRM-12-login-fix"));
    }

    @Test
    void lowercaseNotMatched() {
        assertTrue(ScmCommitLinker.extractTaskKeys("scrm-12 küçük harf").isEmpty());
    }

    @Test
    void numberSuffixNotSplit() {
        // SCRM-123'ün "SCRM-12" olarak kısmen eşleşmemesi gerekir
        assertEquals(Set.of("SCRM-123"), ScmCommitLinker.extractTaskKeys("SCRM-123 fix"));
    }

    @Test
    void emptyAndNullSafe() {
        assertTrue(ScmCommitLinker.extractTaskKeys(null).isEmpty());
        assertTrue(ScmCommitLinker.extractTaskKeys("").isEmpty());
        assertTrue(ScmCommitLinker.extractTaskKeys("anahtar içermeyen mesaj").isEmpty());
    }

    @Test
    void branchNameFormatMatched() {
        assertEquals(Set.of("SCRM-123"),
                ScmCommitLinker.extractTaskKeys("feature/SCRM-123-login-sayfasi-hatasi"));
        assertEquals(Set.of("PAY-1"),
                ScmCommitLinker.extractTaskKeys("bugfix/PAY-1-hotfix"));
    }
}
