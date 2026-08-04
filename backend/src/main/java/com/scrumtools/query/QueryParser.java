package com.scrumtools.query;

import java.util.ArrayList;
import java.util.List;

/**
 * STQL özyinelemeli-inişli (recursive descent) çözümleyici.
 *
 * <pre>
 * query      := orExpr? orderBy?
 * orExpr     := andExpr (OR andExpr)*
 * andExpr    := notExpr (AND notExpr)*
 * notExpr    := NOT notExpr | primary
 * primary    := "(" orExpr ")" | condition
 * condition  := field operator operand
 * operand    := literal | "(" literal ("," literal)* ")" | function
 * orderBy    := ORDER BY field (ASC|DESC)? ("," field (ASC|DESC)?)*
 * </pre>
 *
 * Öncelik: NOT > AND > OR. Yani {@code A AND B OR C} → {@code (A AND B) OR C}.
 */
public final class QueryParser {

    /** Tek sorguda izin verilen azami koşul sayısı. */
    public static final int MAX_CONDITIONS = 50;
    /** Parantezle kurulabilecek azami iç içe derinlik. */
    public static final int MAX_DEPTH = 10;

    private final List<QueryToken> tokens;
    private int index = 0;
    private int conditionCount = 0;

    private QueryParser(List<QueryToken> tokens) {
        this.tokens = tokens;
    }

    public static ParsedQuery parse(String stql) {
        if (stql == null || stql.isBlank()) {
            return ParsedQuery.empty();
        }
        return new QueryParser(QueryLexer.tokenize(stql)).parseQuery();
    }

    private ParsedQuery parseQuery() {
        QueryNode where = null;
        if (!check(QueryTokenType.KW_ORDER) && !check(QueryTokenType.EOF)) {
            where = parseOr(0);
        }

        List<ParsedQuery.OrderBy> orderBy = List.of();
        if (check(QueryTokenType.KW_ORDER)) {
            orderBy = parseOrderBy();
        }

        if (!check(QueryTokenType.EOF)) {
            QueryToken t = peek();
            throw new QueryParseException(
                    "Sorgunun sonunda beklenmeyen ifade: '" + t.text() + "'. "
                            + "Koşullar arasında AND / OR kullanmayı unutmuş olabilirsiniz.", t);
        }
        return new ParsedQuery(where, orderBy);
    }

    // ─── Mantıksal operatörler ────────────────────────────────────────────────

    private QueryNode parseOr(int depth) {
        QueryNode left = parseAnd(depth);
        if (!check(QueryTokenType.KW_OR)) return left;

        List<QueryNode> children = new ArrayList<>();
        children.add(left);
        while (match(QueryTokenType.KW_OR)) {
            children.add(parseAnd(depth));
        }
        return new QueryNode.Or(children);
    }

    private QueryNode parseAnd(int depth) {
        QueryNode left = parseNot(depth);
        if (!check(QueryTokenType.KW_AND)) return left;

        List<QueryNode> children = new ArrayList<>();
        children.add(left);
        while (match(QueryTokenType.KW_AND)) {
            children.add(parseNot(depth));
        }
        return new QueryNode.And(children);
    }

    private QueryNode parseNot(int depth) {
        if (match(QueryTokenType.KW_NOT)) {
            return new QueryNode.Not(parseNot(depth));
        }
        return parsePrimary(depth);
    }

    private QueryNode parsePrimary(int depth) {
        if (depth >= MAX_DEPTH) {
            throw new QueryParseException(
                    "Sorgu çok derin iç içe geçmiş (en fazla " + MAX_DEPTH + " seviye).", peek());
        }
        if (match(QueryTokenType.LPAREN)) {
            QueryNode inner = parseOr(depth + 1);
            expect(QueryTokenType.RPAREN, "Kapanış parantezi ')' bekleniyor.");
            return inner;
        }
        return parseCondition();
    }

    // ─── Koşul ────────────────────────────────────────────────────────────────

    private QueryNode parseCondition() {
        if (++conditionCount > MAX_CONDITIONS) {
            throw new QueryParseException(
                    "Sorguda çok fazla koşul var (en fazla " + MAX_CONDITIONS + ").", peek());
        }

        QueryToken fieldToken = peek();
        String field = parseFieldName();

        QueryOperator op = parseOperator();

        if (op.isUnary()) {
            return new QueryNode.Condition(field, op, List.of(),
                    fieldToken.position(), fieldToken.length());
        }

        List<QueryNode.ValueExpr> values = op.isMultiValue() ? parseValueList() : List.of(parseValue());
        return new QueryNode.Condition(field, op, values,
                fieldToken.position(), fieldToken.length());
    }

    /**
     * Alan adı: düz tanımlayıcı ya da köşeli parantezli biçim —
     * {@code cf[alanAnahtari]} (özel alan), {@code smart["zengin filtre"]} (sınıflandırma).
     */
    private String parseFieldName() {
        QueryToken t = peek();
        if (!t.is(QueryTokenType.IDENT)) {
            throw new QueryParseException(
                    "Alan adı bekleniyordu ama '" + describe(t) + "' bulundu.", t);
        }
        advance();

        // cf[environment] → "cf[environment]"
        if (check(QueryTokenType.LBRACKET)) {
            advance();
            QueryToken key = peek();
            if (!key.is(QueryTokenType.IDENT) && !key.is(QueryTokenType.STRING)) {
                throw new QueryParseException(
                        "Köşeli parantez içinde anahtar bekleniyor: cf[alanAdi] veya smart[\"zengin filtre\"]", key);
            }
            advance();
            expect(QueryTokenType.RBRACKET, "Özel alan için kapanış ']' bekleniyor.");
            return t.text() + "[" + key.text() + "]";
        }
        return t.text();
    }

    private QueryOperator parseOperator() {
        QueryToken t = peek();
        switch (t.type()) {
            case OP_EQ, OP_NEQ, OP_CONTAINS, OP_NOT_CONTAINS, OP_GT, OP_GTE, OP_LT, OP_LTE -> {
                advance();
                return QueryOperator.fromSymbol(t.text())
                        .orElseThrow(() -> new QueryParseException("Bilinmeyen operatör: " + t.text(), t));
            }
            case KW_IN -> {
                advance();
                return QueryOperator.IN;
            }
            case KW_NOT -> {
                advance();
                expect(QueryTokenType.KW_IN, "NOT'tan sonra IN bekleniyor (NOT IN).");
                return QueryOperator.NOT_IN;
            }
            case KW_IS -> {
                advance();
                boolean negated = match(QueryTokenType.KW_NOT);
                if (!match(QueryTokenType.KW_EMPTY) && !match(QueryTokenType.KW_NULL)) {
                    throw new QueryParseException("IS'ten sonra EMPTY veya NULL bekleniyor.", peek());
                }
                return negated ? QueryOperator.IS_NOT_EMPTY : QueryOperator.IS_EMPTY;
            }
            default -> throw new QueryParseException(
                    "Operatör bekleniyordu ama '" + describe(t) + "' bulundu. "
                            + "Kullanılabilir operatörler: = != ~ !~ > >= < <= IN, NOT IN, IS EMPTY.", t);
        }
    }

    /** IN (...) listesi. */
    private List<QueryNode.ValueExpr> parseValueList() {
        expect(QueryTokenType.LPAREN, "IN operatöründen sonra '(' bekleniyor.");
        List<QueryNode.ValueExpr> values = new ArrayList<>();
        if (!check(QueryTokenType.RPAREN)) {
            do {
                values.add(parseValue());
            } while (match(QueryTokenType.COMMA));
        }
        expect(QueryTokenType.RPAREN, "IN listesi için kapanış ')' bekleniyor.");
        if (values.isEmpty()) {
            throw new QueryParseException("IN listesi boş olamaz.", previous());
        }
        return values;
    }

    private QueryNode.ValueExpr parseValue() {
        QueryToken t = peek();
        switch (t.type()) {
            case STRING, NUMBER, RELATIVE_DATE -> {
                advance();
                return new QueryNode.Literal(t.text(), t.type(), t.position(), t.length());
            }
            case KW_EMPTY, KW_NULL -> {
                advance();
                return new QueryNode.Literal("", QueryTokenType.STRING, t.position(), t.length());
            }
            case IDENT -> {
                advance();
                // Fonksiyon çağrısı mı, tırnaksız değer mi?
                if (check(QueryTokenType.LPAREN)) {
                    return parseFunctionArgs(t);
                }
                return new QueryNode.Literal(t.text(), QueryTokenType.IDENT, t.position(), t.length());
            }
            default -> throw new QueryParseException(
                    "Değer bekleniyordu ama '" + describe(t) + "' bulundu.", t);
        }
    }

    private QueryNode.FunctionCall parseFunctionArgs(QueryToken nameToken) {
        expect(QueryTokenType.LPAREN, "Fonksiyon için '(' bekleniyor.");
        List<String> args = new ArrayList<>();
        if (!check(QueryTokenType.RPAREN)) {
            do {
                QueryToken arg = peek();
                if (!arg.is(QueryTokenType.STRING) && !arg.is(QueryTokenType.NUMBER) && !arg.is(QueryTokenType.IDENT)) {
                    throw new QueryParseException("Geçersiz fonksiyon argümanı.", arg);
                }
                args.add(arg.text());
                advance();
            } while (match(QueryTokenType.COMMA));
        }
        QueryToken close = peek();
        expect(QueryTokenType.RPAREN, "Fonksiyon için kapanış ')' bekleniyor.");
        int length = close.position() + close.length() - nameToken.position();
        return new QueryNode.FunctionCall(nameToken.text(), args, nameToken.position(), length);
    }

    // ─── ORDER BY ─────────────────────────────────────────────────────────────

    private List<ParsedQuery.OrderBy> parseOrderBy() {
        expect(QueryTokenType.KW_ORDER, "ORDER bekleniyor.");
        expect(QueryTokenType.KW_BY, "ORDER'dan sonra BY bekleniyor.");

        List<ParsedQuery.OrderBy> list = new ArrayList<>();
        do {
            QueryToken t = peek();
            String field = parseFieldName();
            boolean desc = false;
            if (match(QueryTokenType.KW_DESC)) desc = true;
            else match(QueryTokenType.KW_ASC);
            list.add(new ParsedQuery.OrderBy(field, desc, t.position(), t.length()));
        } while (match(QueryTokenType.COMMA));
        return list;
    }

    // ─── Token yardımcıları ───────────────────────────────────────────────────

    private QueryToken peek() {
        return tokens.get(index);
    }

    private QueryToken previous() {
        return tokens.get(Math.max(0, index - 1));
    }

    private void advance() {
        if (index < tokens.size() - 1) index++;
    }

    private boolean check(QueryTokenType type) {
        return peek().type() == type;
    }

    private boolean match(QueryTokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(QueryTokenType type, String message) {
        if (!match(type)) {
            throw new QueryParseException(message, peek());
        }
    }

    private static String describe(QueryToken t) {
        return t.is(QueryTokenType.EOF) ? "sorgu sonu" : t.text();
    }
}
