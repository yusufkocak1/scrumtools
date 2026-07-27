package com.scrumtools.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryLexerTest {

    @Test
    @DisplayName("Alan, operatör ve tırnaklı değer ayrı token'lara bölünür")
    void tokenizesSimpleCondition() {
        List<QueryToken> tokens = QueryLexer.tokenize("summary ~ \"ödeme\"");

        assertEquals(4, tokens.size()); // + EOF
        assertEquals(QueryTokenType.IDENT, tokens.get(0).type());
        assertEquals("summary", tokens.get(0).text());
        assertEquals(QueryTokenType.OP_CONTAINS, tokens.get(1).type());
        assertEquals(QueryTokenType.STRING, tokens.get(2).type());
        assertEquals("ödeme", tokens.get(2).text(), "tırnaklar soyulmalı");
        assertEquals(QueryTokenType.EOF, tokens.get(3).type());
    }

    @Test
    @DisplayName("Anahtar kelimeler büyük/küçük harf duyarsızdır")
    void keywordsAreCaseInsensitive() {
        List<QueryToken> tokens = QueryLexer.tokenize("a = 1 and b = 2 Or c = 3");

        assertTrue(tokens.stream().anyMatch(t -> t.type() == QueryTokenType.KW_AND));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == QueryTokenType.KW_OR));
    }

    @Test
    @DisplayName("Çift karakterli operatörler tek token olur")
    void readsTwoCharOperators() {
        assertEquals(QueryTokenType.OP_NEQ, first("a != 1"));
        assertEquals(QueryTokenType.OP_NOT_CONTAINS, first("a !~ 1"));
        assertEquals(QueryTokenType.OP_GTE, first("a >= 1"));
        assertEquals(QueryTokenType.OP_LTE, first("a <= 1"));
        assertEquals(QueryTokenType.OP_NEQ, first("a <> 1"), "<> de eşit-değil sayılır");
    }

    @Test
    @DisplayName("Göreli tarih literal'i sayıdan ayrılır")
    void distinguishesRelativeDateFromNumber() {
        List<QueryToken> rel = QueryLexer.tokenize("-7d");
        assertEquals(QueryTokenType.RELATIVE_DATE, rel.get(0).type());
        assertEquals("-7d", rel.get(0).text());

        List<QueryToken> num = QueryLexer.tokenize("7");
        assertEquals(QueryTokenType.NUMBER, num.get(0).type());

        // Birimden sonra harf devam ediyorsa tanımlayıcıdır, göreli tarih değil.
        List<QueryToken> ident = QueryLexer.tokenize("3days");
        assertEquals(QueryTokenType.NUMBER, ident.get(0).type());
        assertEquals("3", ident.get(0).text());
    }

    @Test
    @DisplayName("Token'lar kaynak metindeki konumlarını taşır")
    void tokensCarrySourcePosition() {
        List<QueryToken> tokens = QueryLexer.tokenize("status = Done");

        assertEquals(0, tokens.get(0).position());
        assertEquals(6, tokens.get(0).length());
        assertEquals(7, tokens.get(1).position());
        assertEquals(9, tokens.get(2).position());
    }

    @Test
    @DisplayName("Kapatılmamış tırnak konumuyla birlikte hata verir")
    void reportsUnterminatedString() {
        QueryParseException e = assertThrows(QueryParseException.class,
                () -> QueryLexer.tokenize("summary ~ \"ödeme"));

        assertTrue(e.getMessage().contains("tırnak"));
        assertEquals(10, e.getPosition());
    }

    @Test
    @DisplayName("Kaçış dizileri çözülür")
    void resolvesEscapeSequences() {
        List<QueryToken> tokens = QueryLexer.tokenize("summary ~ \"a\\\"b\"");
        assertEquals("a\"b", tokens.get(2).text());
    }

    @Test
    @DisplayName("Azami uzunluğu aşan sorgu reddedilir")
    void rejectsOverlongQuery() {
        String tooLong = "a".repeat(QueryLexer.MAX_QUERY_LENGTH + 1);
        assertThrows(QueryParseException.class, () -> QueryLexer.tokenize(tooLong));
    }

    private static QueryTokenType first(String input) {
        return QueryLexer.tokenize(input).get(1).type();
    }
}
