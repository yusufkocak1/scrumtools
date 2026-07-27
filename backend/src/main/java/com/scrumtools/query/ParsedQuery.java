package com.scrumtools.query;

import java.util.List;

/**
 * Çözümlenmiş bir STQL sorgusu: WHERE ağacı + ORDER BY listesi.
 *
 * @param where   koşul ağacı — sorgu yalnız "ORDER BY ..." ise null olabilir
 * @param orderBy sıralama listesi; boşsa çağıran varsayılanını uygular
 */
public record ParsedQuery(QueryNode where, List<OrderBy> orderBy) {

    /** Tek bir sıralama maddesi. */
    public record OrderBy(String field, boolean descending, int position, int length) {}

    public boolean hasWhere() {
        return where != null;
    }

    public boolean hasOrderBy() {
        return orderBy != null && !orderBy.isEmpty();
    }

    public static ParsedQuery empty() {
        return new ParsedQuery(null, List.of());
    }
}
