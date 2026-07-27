package com.scrumtools.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskFieldRegistryTest {

    @Test
    @DisplayName("Jira adları ve entity alan adları aynı tanıma çözülür")
    void resolvesAliasesToSameField() {
        FieldDescriptor bySummary = TaskFieldRegistry.resolve("summary").orElseThrow();
        FieldDescriptor byTitle = TaskFieldRegistry.resolve("title").orElseThrow();

        assertEquals(bySummary, byTitle);
        assertEquals("title", bySummary.path(), "her ikisi de Task.title alanına gitmeli");
    }

    @Test
    @DisplayName("Alan adları büyük/küçük harf duyarsızdır")
    void resolutionIsCaseInsensitive() {
        assertTrue(TaskFieldRegistry.resolve("STATUS").isPresent());
        assertTrue(TaskFieldRegistry.resolve("IssueType").isPresent());
    }

    @Test
    @DisplayName("Eski filters[] alan adları hâlâ tanınır")
    void keepsLegacyFieldNames() {
        // Mevcut arayüz bu adları gönderiyor; kırılmamalı.
        for (String legacy : new String[]{"status", "priority", "issueType", "assignee",
                "reporter", "labels", "dueDate", "startDate", "sprintId"}) {
            assertTrue(TaskFieldRegistry.resolve(legacy).isPresent(), legacy + " çözülemedi");
        }
    }

    @Test
    @DisplayName("cf[anahtar] dinamik özel alana çözülür")
    void resolvesCustomField() {
        FieldDescriptor f = TaskFieldRegistry.resolve("cf[fixVersion]").orElseThrow();

        assertEquals(FieldType.CUSTOM_FIELD, f.type());
        assertEquals("fixVersion", f.path());
    }

    @Test
    @DisplayName("Geçersiz özel alan söz dizimi çözülmez")
    void rejectsMalformedCustomField() {
        assertTrue(TaskFieldRegistry.resolve("cf[]").isEmpty());
        assertTrue(TaskFieldRegistry.resolve("xx[abc]").isEmpty());
    }

    @Test
    @DisplayName("Bilinmeyen alan için yakın öneri üretilir")
    void suggestsSimilarFieldOnTypo() {
        assertEquals(Optional.of("status"), TaskFieldRegistry.suggestSimilar("statuss"));
        assertEquals(Optional.of("assignee"), TaskFieldRegistry.suggestSimilar("assigne"));
    }

    @Test
    @DisplayName("Alakasız girdi için öneri üretilmez")
    void doesNotSuggestForUnrelatedInput() {
        assertTrue(TaskFieldRegistry.suggestSimilar("zzzzzzzzz").isEmpty());
    }

    @Test
    @DisplayName("Metin alanı sayısal operatörleri kabul etmez")
    void enforcesOperatorCompatibility() {
        FieldDescriptor summary = TaskFieldRegistry.resolve("summary").orElseThrow();
        assertTrue(summary.supports(QueryOperator.CONTAINS));
        assertFalse(summary.supports(QueryOperator.GT));

        FieldDescriptor points = TaskFieldRegistry.resolve("storyPoints").orElseThrow();
        assertTrue(points.supports(QueryOperator.GT));
        assertFalse(points.supports(QueryOperator.CONTAINS));
    }

    @Test
    @DisplayName("Katalogdaki her alanın etiketi ve yolu vardır")
    void catalogIsComplete() {
        for (FieldDescriptor f : TaskFieldRegistry.all()) {
            assertNotNull(f.label(), f.name() + " etiketsiz");
            assertFalse(f.label().isBlank(), f.name() + " etiketi boş");
            assertNotNull(f.path(), f.name() + " yolu tanımsız");
            assertFalse(f.type().operators().isEmpty(), f.name() + " operatörsüz");
        }
    }
}
