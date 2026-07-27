package com.scrumtools.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    @Test
    @DisplayName("Boş sorgu koşulsuz sonuç verir")
    void emptyQueryHasNoWhere() {
        assertFalse(QueryParser.parse("").hasWhere());
        assertFalse(QueryParser.parse("   ").hasWhere());
        assertFalse(QueryParser.parse(null).hasWhere());
    }

    @Test
    @DisplayName("Tek koşul Condition düğümüne çözülür")
    void parsesSingleCondition() {
        ParsedQuery q = QueryParser.parse("summary ~ \"ödeme\"");

        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class, q.where());
        assertEquals("summary", c.field());
        assertEquals(QueryOperator.CONTAINS, c.operator());
        assertEquals(1, c.values().size());
        assertEquals("ödeme", ((QueryNode.Literal) c.values().get(0)).text());
    }

    @Test
    @DisplayName("AND, OR'dan önce bağlar: A AND B OR C → (A AND B) OR C")
    void andBindsTighterThanOr() {
        ParsedQuery q = QueryParser.parse("a = 1 AND b = 2 OR c = 3");

        QueryNode.Or or = assertInstanceOf(QueryNode.Or.class, q.where());
        assertEquals(2, or.children().size());
        assertInstanceOf(QueryNode.And.class, or.children().get(0));
        assertInstanceOf(QueryNode.Condition.class, or.children().get(1));
    }

    @Test
    @DisplayName("Parantez öncelik sırasını değiştirir")
    void parenthesesOverridePrecedence() {
        ParsedQuery q = QueryParser.parse("a = 1 AND (b = 2 OR c = 3)");

        QueryNode.And and = assertInstanceOf(QueryNode.And.class, q.where());
        assertEquals(2, and.children().size());
        assertInstanceOf(QueryNode.Or.class, and.children().get(1));
    }

    @Test
    @DisplayName("Aynı seviyedeki zincir tek düğümde toplanır")
    void flattensSameLevelChain() {
        QueryNode.And and = assertInstanceOf(QueryNode.And.class,
                QueryParser.parse("a = 1 AND b = 2 AND c = 3").where());
        assertEquals(3, and.children().size());
    }

    @Test
    @DisplayName("NOT en sıkı bağlar")
    void notBindsTightest() {
        QueryNode.And and = assertInstanceOf(QueryNode.And.class,
                QueryParser.parse("NOT a = 1 AND b = 2").where());
        assertInstanceOf(QueryNode.Not.class, and.children().get(0));
        assertInstanceOf(QueryNode.Condition.class, and.children().get(1));
    }

    @Test
    @DisplayName("IN listesi çoklu değer taşır")
    void parsesInList() {
        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("status IN (\"To Do\", \"In Progress\")").where());

        assertEquals(QueryOperator.IN, c.operator());
        assertEquals(2, c.values().size());
        assertEquals("In Progress", ((QueryNode.Literal) c.values().get(1)).text());
    }

    @Test
    @DisplayName("NOT IN tek operatör olarak okunur")
    void parsesNotIn() {
        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("status NOT IN (Done)").where());
        assertEquals(QueryOperator.NOT_IN, c.operator());
    }

    @Test
    @DisplayName("IS EMPTY ve IS NOT EMPTY değersiz operatörlerdir")
    void parsesEmptyOperators() {
        QueryNode.Condition empty = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("assignee IS EMPTY").where());
        assertEquals(QueryOperator.IS_EMPTY, empty.operator());
        assertTrue(empty.values().isEmpty());

        QueryNode.Condition notEmpty = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("assignee IS NOT NULL").where());
        assertEquals(QueryOperator.IS_NOT_EMPTY, notEmpty.operator(), "NULL, EMPTY ile eş anlamlı");
    }

    @Test
    @DisplayName("Fonksiyon çağrısı ayrı düğüm tipidir")
    void parsesFunctionCall() {
        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("assignee = currentUser()").where());

        QueryNode.FunctionCall fn = assertInstanceOf(QueryNode.FunctionCall.class, c.values().get(0));
        assertEquals("currentUser", fn.name());
        assertTrue(fn.args().isEmpty());
    }

    @Test
    @DisplayName("ORDER BY yönüyle birlikte okunur, varsayılan ASC'tir")
    void parsesOrderBy() {
        ParsedQuery q = QueryParser.parse("status = Done ORDER BY priority DESC, created");

        assertTrue(q.hasOrderBy());
        assertEquals(2, q.orderBy().size());
        assertEquals("priority", q.orderBy().get(0).field());
        assertTrue(q.orderBy().get(0).descending());
        assertEquals("created", q.orderBy().get(1).field());
        assertFalse(q.orderBy().get(1).descending());
    }

    @Test
    @DisplayName("Koşulsuz ORDER BY tek başına geçerlidir")
    void parsesOrderByWithoutWhere() {
        ParsedQuery q = QueryParser.parse("ORDER BY created DESC");
        assertFalse(q.hasWhere());
        assertTrue(q.hasOrderBy());
    }

    @Test
    @DisplayName("Özel alan söz dizimi cf[anahtar] korunur")
    void parsesCustomFieldSyntax() {
        QueryNode.Condition c = assertInstanceOf(QueryNode.Condition.class,
                QueryParser.parse("cf[fixVersion] = \"1.2\"").where());
        assertEquals("cf[fixVersion]", c.field());
    }

    // ─── Hata durumları ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Eksik operatör hatanın konumunu bildirir")
    void reportsMissingOperator() {
        QueryParseException e = assertThrows(QueryParseException.class,
                () -> QueryParser.parse("status Done"));
        assertTrue(e.getMessage().contains("Operatör"));
        assertEquals(7, e.getPosition());
    }

    @Test
    @DisplayName("Çift operatör hata verir")
    void reportsDoubleOperator() {
        QueryParseException e = assertThrows(QueryParseException.class,
                () -> QueryParser.parse("status = = Done"));
        assertTrue(e.getPosition() > 0);
    }

    @Test
    @DisplayName("Kapatılmamış parantez hata verir")
    void reportsUnclosedParen() {
        assertThrows(QueryParseException.class, () -> QueryParser.parse("(a = 1"));
    }

    @Test
    @DisplayName("Bağlaçsız ard arda koşul hata verir")
    void reportsMissingConjunction() {
        QueryParseException e = assertThrows(QueryParseException.class,
                () -> QueryParser.parse("a = 1 b = 2"));
        assertTrue(e.getMessage().contains("AND"));
    }

    @Test
    @DisplayName("Koşul sayısı sınırı aşılamaz")
    void enforcesConditionLimit() {
        StringBuilder sb = new StringBuilder("a = 1");
        for (int i = 0; i < QueryParser.MAX_CONDITIONS; i++) sb.append(" AND a = 1");

        QueryParseException e = assertThrows(QueryParseException.class,
                () -> QueryParser.parse(sb.toString()));
        assertTrue(e.getMessage().contains("çok fazla koşul"));
    }

    @Test
    @DisplayName("İç içe parantez derinliği sınırı aşılamaz")
    void enforcesDepthLimit() {
        String deep = "(".repeat(QueryParser.MAX_DEPTH + 2) + "a = 1"
                + ")".repeat(QueryParser.MAX_DEPTH + 2);

        QueryParseException e = assertThrows(QueryParseException.class, () -> QueryParser.parse(deep));
        assertTrue(e.getMessage().contains("derin"));
    }

    @Test
    @DisplayName("Boş IN listesi reddedilir")
    void rejectsEmptyInList() {
        assertThrows(QueryParseException.class, () -> QueryParser.parse("status IN ()"));
    }
}
