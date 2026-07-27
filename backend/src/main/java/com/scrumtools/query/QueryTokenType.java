package com.scrumtools.query;

/** STQL lexer'ının ürettiği token türleri. */
public enum QueryTokenType {
    /** Alan adı veya fonksiyon adı — tırnaksız tanımlayıcı. */
    IDENT,
    /** Tırnaklı metin: "ödeme" veya 'ödeme'. */
    STRING,
    /** Tam sayı veya ondalık: 5, 3.5 */
    NUMBER,
    /** Göreli tarih: -7d, 2w, -1M (bkz. QueryLexer#readRelativeDate). */
    RELATIVE_DATE,

    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    COMMA,

    OP_EQ, OP_NEQ,
    OP_CONTAINS, OP_NOT_CONTAINS,
    OP_GT, OP_GTE, OP_LT, OP_LTE,

    KW_AND, KW_OR, KW_NOT,
    KW_IN, KW_IS,
    KW_EMPTY, KW_NULL,
    KW_ORDER, KW_BY, KW_ASC, KW_DESC,

    EOF
}
