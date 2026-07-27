package com.scrumtools.query;

import java.util.List;

/**
 * STQL soyut sözdizim ağacı.
 *
 * Ağaç yalnızca yapıyı taşır — alan çözümü {@link TaskFieldRegistry},
 * SQL'e çeviri {@link QueryPredicateBuilder} sorumluluğundadır.
 */
public sealed interface QueryNode
        permits QueryNode.And, QueryNode.Or, QueryNode.Not, QueryNode.Condition {

    record And(List<QueryNode> children) implements QueryNode {}

    record Or(List<QueryNode> children) implements QueryNode {}

    record Not(QueryNode child) implements QueryNode {}

    /**
     * Tek karşılaştırma: {@code assignee = currentUser()}.
     *
     * @param field    kullanıcının yazdığı alan adı (alias çözülmemiş hâli)
     * @param operator karşılaştırma operatörü
     * @param values   sağ taraf — unary operatörlerde boş
     * @param position hata raporlaması için alanın kaynak metindeki konumu
     * @param length   alan adının uzunluğu
     */
    record Condition(
            String field,
            QueryOperator operator,
            List<ValueExpr> values,
            int position,
            int length
    ) implements QueryNode {}

    /** Sağ taraftaki tek bir değer: ya düz literal ya fonksiyon çağrısı. */
    sealed interface ValueExpr permits Literal, FunctionCall {
        int position();
        int length();
    }

    /** Düz değer: "ödeme", 5, 2026-01-15, -7d */
    record Literal(String text, QueryTokenType tokenType, int position, int length) implements ValueExpr {}

    /** Fonksiyon çağrısı: currentUser(), startOfWeek(), currentSprint() */
    record FunctionCall(String name, List<String> args, int position, int length) implements ValueExpr {}
}
