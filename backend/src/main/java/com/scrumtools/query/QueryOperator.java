package com.scrumtools.query;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** STQL karşılaştırma operatörleri. */
public enum QueryOperator {
    EQ("="),
    NEQ("!="),
    CONTAINS("~"),
    NOT_CONTAINS("!~"),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    IN("IN"),
    NOT_IN("NOT IN"),
    IS_EMPTY("IS EMPTY"),
    IS_NOT_EMPTY("IS NOT EMPTY");

    private final String symbol;

    QueryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /** Değer beklemeyen operatörler (sağ tarafı yoktur). */
    public boolean isUnary() {
        return this == IS_EMPTY || this == IS_NOT_EMPTY;
    }

    /** Birden çok değer alan operatörler. */
    public boolean isMultiValue() {
        return this == IN || this == NOT_IN;
    }

    private static final Map<String, QueryOperator> BY_SYMBOL = Map.ofEntries(
            Map.entry("=", EQ),
            Map.entry("!=", NEQ),
            Map.entry("<>", NEQ),
            Map.entry("~", CONTAINS),
            Map.entry("!~", NOT_CONTAINS),
            Map.entry(">", GT),
            Map.entry(">=", GTE),
            Map.entry("<", LT),
            Map.entry("<=", LTE),
            Map.entry("IN", IN),
            Map.entry("NOT IN", NOT_IN),
            Map.entry("IS EMPTY", IS_EMPTY),
            Map.entry("IS NOT EMPTY", IS_NOT_EMPTY)
    );

    /** Eski filters[] API'sinden gelen operatör adlarının karşılığı (geriye dönük uyum). */
    private static final Map<String, QueryOperator> LEGACY = Map.of(
            "eq", EQ,
            "neq", NEQ,
            "in", IN,
            "contains", CONTAINS,
            "gt", GT,
            "lt", LT,
            "is_null", IS_EMPTY,
            "is_not_null", IS_NOT_EMPTY
    );

    public static Optional<QueryOperator> fromSymbol(String symbol) {
        if (symbol == null) return Optional.empty();
        return Optional.ofNullable(BY_SYMBOL.get(symbol.trim().toUpperCase(Locale.ROOT)));
    }

    public static Optional<QueryOperator> fromLegacy(String legacyName) {
        if (legacyName == null) return Optional.empty();
        return Optional.ofNullable(LEGACY.get(legacyName.trim().toLowerCase(Locale.ROOT)));
    }
}
