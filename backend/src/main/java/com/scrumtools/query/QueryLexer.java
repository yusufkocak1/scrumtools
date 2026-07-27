package com.scrumtools.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * STQL sözcüksel çözümleyici — karakter akışını token listesine çevirir.
 *
 * Anahtar kelimeler büyük/küçük harf duyarsızdır (AND = and = And); alan adları
 * ve değerler ham hâliyle taşınır, çözümleme {@link TaskFieldRegistry} işidir.
 */
public final class QueryLexer {

    /** Tek bir sorgunun kabul edilen azami uzunluğu — kötü niyetli/kazara devasa girdiye karşı. */
    public static final int MAX_QUERY_LENGTH = 2000;

    private static final Map<String, QueryTokenType> KEYWORDS = Map.ofEntries(
            Map.entry("AND", QueryTokenType.KW_AND),
            Map.entry("OR", QueryTokenType.KW_OR),
            Map.entry("NOT", QueryTokenType.KW_NOT),
            Map.entry("IN", QueryTokenType.KW_IN),
            Map.entry("IS", QueryTokenType.KW_IS),
            Map.entry("EMPTY", QueryTokenType.KW_EMPTY),
            Map.entry("NULL", QueryTokenType.KW_NULL),
            Map.entry("ORDER", QueryTokenType.KW_ORDER),
            Map.entry("BY", QueryTokenType.KW_BY),
            Map.entry("ASC", QueryTokenType.KW_ASC),
            Map.entry("DESC", QueryTokenType.KW_DESC)
    );

    private final String src;
    private int pos = 0;

    private QueryLexer(String src) {
        this.src = src;
    }

    public static List<QueryToken> tokenize(String input) {
        String src = input == null ? "" : input;
        if (src.length() > MAX_QUERY_LENGTH) {
            throw new QueryParseException(
                    "Sorgu çok uzun (en fazla " + MAX_QUERY_LENGTH + " karakter).",
                    MAX_QUERY_LENGTH, 1);
        }
        return new QueryLexer(src).scan();
    }

    private List<QueryToken> scan() {
        List<QueryToken> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) break;
            tokens.add(nextToken());
        }
        tokens.add(new QueryToken(QueryTokenType.EOF, "", src.length(), 1));
        return tokens;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private QueryToken nextToken() {
        int start = pos;
        char c = src.charAt(pos);

        switch (c) {
            case '(': pos++; return token(QueryTokenType.LPAREN, "(", start);
            case ')': pos++; return token(QueryTokenType.RPAREN, ")", start);
            case '[': pos++; return token(QueryTokenType.LBRACKET, "[", start);
            case ']': pos++; return token(QueryTokenType.RBRACKET, "]", start);
            case ',': pos++; return token(QueryTokenType.COMMA, ",", start);
            case '"':
            case '\'': return readString(c);
            case '=':
                pos++;
                // "==" de kabul edilir; kullanıcı JQL dışı alışkanlıkla yazabilir.
                if (peek() == '=') pos++;
                return token(QueryTokenType.OP_EQ, "=", start);
            case '~': pos++; return token(QueryTokenType.OP_CONTAINS, "~", start);
            case '!':
                pos++;
                if (peek() == '=') { pos++; return token(QueryTokenType.OP_NEQ, "!=", start); }
                if (peek() == '~') { pos++; return token(QueryTokenType.OP_NOT_CONTAINS, "!~", start); }
                throw new QueryParseException("'!' tek başına kullanılamaz; '!=' veya '!~' bekleniyor.", start, 1);
            case '<':
                pos++;
                if (peek() == '=') { pos++; return token(QueryTokenType.OP_LTE, "<=", start); }
                if (peek() == '>') { pos++; return token(QueryTokenType.OP_NEQ, "<>", start); }
                return token(QueryTokenType.OP_LT, "<", start);
            case '>':
                pos++;
                if (peek() == '=') { pos++; return token(QueryTokenType.OP_GTE, ">=", start); }
                return token(QueryTokenType.OP_GT, ">", start);
            default:
                break;
        }

        if (c == '-' || c == '+' || Character.isDigit(c)) {
            return readNumberOrRelativeDate();
        }
        if (isIdentStart(c)) {
            return readIdentOrKeyword();
        }
        throw new QueryParseException("Beklenmeyen karakter: '" + c + "'", start, 1);
    }

    /** Tırnaklı metin. Kaçış: \" \' \\ ve \n \t. */
    private QueryToken readString(char quote) {
        int start = pos;
        pos++; // açılış tırnağı
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != quote) {
            char ch = src.charAt(pos);
            if (ch == '\\' && pos + 1 < src.length()) {
                pos++;
                char esc = src.charAt(pos);
                sb.append(switch (esc) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> esc;
                });
            } else {
                sb.append(ch);
            }
            pos++;
        }
        if (pos >= src.length()) {
            throw new QueryParseException("Kapatılmamış tırnak.", start, Math.max(1, src.length() - start));
        }
        pos++; // kapanış tırnağı
        return new QueryToken(QueryTokenType.STRING, sb.toString(), start, pos - start);
    }

    /**
     * Sayı ya da göreli tarih. Göreli tarih sayının hemen ardından gelen birim
     * harfiyle ayırt edilir: d=gün, w=hafta, M=ay, y=yıl, h=saat, m=dakika.
     * Örn: -7d (7 gün önce), 2w (2 hafta sonra), -1M (1 ay önce).
     */
    private QueryToken readNumberOrRelativeDate() {
        int start = pos;
        if (src.charAt(pos) == '-' || src.charAt(pos) == '+') pos++;
        boolean seenDigit = false;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; seenDigit = true; }
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; seenDigit = true; }
        }
        if (!seenDigit) {
            throw new QueryParseException("Geçersiz sayı.", start, Math.max(1, pos - start));
        }
        if (pos < src.length() && "dwMyhm".indexOf(src.charAt(pos)) >= 0) {
            // Birimden sonra harf devam ediyorsa bu bir tanımlayıcıdır (ör. 3days) — sayı olarak bırak.
            char unit = src.charAt(pos);
            if (pos + 1 >= src.length() || !isIdentPart(src.charAt(pos + 1))) {
                pos++;
                return new QueryToken(QueryTokenType.RELATIVE_DATE,
                        src.substring(start, pos - 1) + unit, start, pos - start);
            }
        }
        return new QueryToken(QueryTokenType.NUMBER, src.substring(start, pos), start, pos - start);
    }

    private QueryToken readIdentOrKeyword() {
        int start = pos;
        while (pos < src.length() && isIdentPart(src.charAt(pos))) pos++;
        String text = src.substring(start, pos);
        QueryTokenType kw = KEYWORDS.get(text.toUpperCase(Locale.ROOT));
        return new QueryToken(kw != null ? kw : QueryTokenType.IDENT, text, start, pos - start);
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private QueryToken token(QueryTokenType type, String text, int start) {
        return new QueryToken(type, text, start, pos - start);
    }

    /** Alan adları harf/alt çizgi ile başlar; Türkçe karakterler de kabul edilir. */
    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }
}
