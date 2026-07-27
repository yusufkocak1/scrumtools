package com.scrumtools.query;

import com.scrumtools.dto.TaskFilterCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Eski filters[] formatının yeni motora köprülenmesi.
 * Bu testler mevcut arayüzün (FilterBar / FilterBuilder) davranışının korunduğunu doğrular.
 */
class LegacyFilterTranslatorTest {

    @Test
    @DisplayName("Boş filtre listesi koşulsuz sorguya çevrilir")
    void emptyFiltersProduceNoNode() {
        assertNull(LegacyFilterTranslator.toNode(null));
        assertNull(LegacyFilterTranslator.toNode(List.of()));
        assertEquals("", LegacyFilterTranslator.toStql(List.of()));
    }

    @Test
    @DisplayName("Tek filtre tek koşula çevrilir")
    void translatesSingleFilter() {
        QueryNode node = LegacyFilterTranslator.toNode(List.of(criteria("status", "eq", "Done")));

        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class, node);
        assertEquals("status", c.field());
        assertEquals(QueryOperator.EQ, c.operator());
    }

    @Test
    @DisplayName("Çoklu filtre AND ile birleşir — eski davranış korunur")
    void combinesMultipleFiltersWithAnd() {
        QueryNode node = LegacyFilterTranslator.toNode(List.of(
                criteria("status", "eq", "Done"),
                criteria("priority", "eq", "High")));

        QueryNode.And and = assertInstanceOf(QueryNode.And.class, node);
        assertEquals(2, and.children().size());
    }

    @Test
    @DisplayName("Eski operatör adları karşılıklarına çevrilir")
    void mapsLegacyOperatorNames() {
        assertEquals(QueryOperator.EQ, operatorOf("eq"));
        assertEquals(QueryOperator.NEQ, operatorOf("neq"));
        assertEquals(QueryOperator.CONTAINS, operatorOf("contains"));
        assertEquals(QueryOperator.IN, operatorOf("in"));
        assertEquals(QueryOperator.GT, operatorOf("gt"));
        assertEquals(QueryOperator.LT, operatorOf("lt"));
        assertEquals(QueryOperator.IS_EMPTY, operatorOf("is_null"));
        assertEquals(QueryOperator.IS_NOT_EMPTY, operatorOf("is_not_null"));
    }

    @Test
    @DisplayName("Tanınmayan alan ve operatör sessizce atlanır")
    void skipsUnknownFieldsAndOperators() {
        assertNull(LegacyFilterTranslator.toNode(List.of(criteria("bilinmeyen", "eq", "x"))));
        assertNull(LegacyFilterTranslator.toNode(List.of(criteria("status", "bilinmeyen", "x"))));
    }

    @Test
    @DisplayName("Değersiz filtreler atlanır, unary operatörler korunur")
    void handlesMissingValues() {
        assertNull(LegacyFilterTranslator.toNode(List.of(criteria("status", "eq"))));

        QueryNode node = LegacyFilterTranslator.toNode(List.of(criteria("sprintId", "is_null")));
        assertInstanceOf(QueryNode.Condition.class, node);
    }

    // ─── STQL metnine çeviri (arayüzün sekme geçişi) ──────────────────────────

    @Test
    @DisplayName("Basit değerler tırnaksız, boşluklu değerler tırnaklı yazılır")
    void quotesOnlyWhenNeeded() {
        assertEquals("priority = High",
                LegacyFilterTranslator.toStql(List.of(criteria("priority", "eq", "High"))));
        assertEquals("status = \"In Progress\"",
                LegacyFilterTranslator.toStql(List.of(criteria("status", "eq", "In Progress"))));
    }

    @Test
    @DisplayName("Alan adları kanonik STQL adına dönüştürülür")
    void normalizesFieldNames() {
        assertEquals("sprint IS EMPTY",
                LegacyFilterTranslator.toStql(List.of(criteria("sprintId", "is_null"))));
        assertEquals("due > 2026-01-15",
                LegacyFilterTranslator.toStql(List.of(criteria("dueDate", "gt", "2026-01-15"))));
    }

    @Test
    @DisplayName("IN listesi parantezli yazılır")
    void writesInList() {
        TaskFilterCriteria c = new TaskFilterCriteria();
        c.setField("status");
        c.setOperator("in");
        c.setValues(List.of("Done", "In Progress"));

        assertEquals("status IN (Done, \"In Progress\")", LegacyFilterTranslator.toStql(List.of(c)));
    }

    @Test
    @DisplayName("Çoklu filtre AND ile birleştirilmiş metin üretir")
    void joinsWithAnd() {
        String stql = LegacyFilterTranslator.toStql(List.of(
                criteria("status", "eq", "Done"),
                criteria("assignee", "eq", "a@b.com")));

        assertEquals("status = Done AND assignee = \"a@b.com\"", stql);
    }

    @Test
    @DisplayName("Üretilen metin yeniden çözümlenebilir — çeviri kayıpsızdır")
    void producedTextIsParseable() {
        String stql = LegacyFilterTranslator.toStql(List.of(
                criteria("status", "eq", "In Progress"),
                criteria("priority", "in", "High"),
                criteria("sprintId", "is_not_null")));

        assertDoesNotThrow(() -> QueryParser.parse(stql));
    }

    @Test
    @DisplayName("Anahtar kelimeyle çakışan değerler tırnaklanır")
    void quotesReservedWords() {
        assertEquals("status = \"NOT\"",
                LegacyFilterTranslator.toStql(List.of(criteria("status", "eq", "NOT"))));
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private static QueryOperator operatorOf(String legacyOperator) {
        QueryNode node = LegacyFilterTranslator.toNode(List.of(criteria("status", legacyOperator, "x")));
        return assertInstanceOf(QueryNode.Condition.class, node).operator();
    }

    private static TaskFilterCriteria criteria(String field, String operator, String... values) {
        TaskFilterCriteria c = new TaskFilterCriteria();
        c.setField(field);
        c.setOperator(operator);
        c.setValues(List.of(values));
        return c;
    }
}
