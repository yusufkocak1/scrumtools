package com.scrumtools.query;

import java.util.List;

/**
 * Elle kurulan sorgu parçaları — kullanıcının yazdığı metinden değil, arayüzdeki
 * seçimlerden doğan koşullar.
 *
 * Zengin filtre çalışma zamanı, seçimleri STQL metnine çevirip yeniden çözümlemek
 * yerine doğrudan ağaç düğümü üretir. Böylece kaçış/tırnak hataları olmaz ve
 * istemciden gelen değerler hiçbir zaman sorgu metnine dönüşmez
 * (bkz. RICH_FILTER_PLAN.md — K1, K2).
 *
 * Düğüm şekillerinin sahibi bu paket olduğu için fabrika da burada durur;
 * servis katmanı {@link QueryNode} kayıtlarını doğrudan kurmaz.
 */
public final class QueryFragments {

    private QueryFragments() {
    }

    /** {@code smart["<zengin filtre>"] IN ("A", "B")} */
    public static ParsedQuery smartIn(String richFilterName, List<String> clauseNames) {
        String field = TaskFieldRegistry.SMART_FILTER_PREFIX + "[" + richFilterName + "]";
        return single(condition(field, QueryOperator.IN, clauseNames));
    }

    /** {@code <alan> IN ("A", "B")} — dinamik filtre seçimleri için. */
    public static ParsedQuery fieldIn(String field, List<String> values) {
        return single(condition(field, QueryOperator.IN, values));
    }

    /**
     * {@code <alan> IS EMPTY} — dinamik filtrede "(Boş)" seçeneği.
     *
     * Boş kova, değeri olmayan kayıtları temsil eder; {@code IN ("")} ile
     * aranamaz çünkü SQL'de NULL hiçbir değere eşit değildir.
     */
    public static ParsedQuery fieldIsEmpty(String field) {
        return single(condition(field, QueryOperator.IS_EMPTY, List.of()));
    }

    /**
     * Serbest metin araması: başlıkta <b>veya</b> görev numarasında geçenler.
     *
     * Açıklama bilinçli olarak dışarıda: uzun metin taraması kısa bir arama
     * kutusundan beklenen hızda dönmez ve sonuçları çoğu zaman gürültülü olur.
     */
    public static ParsedQuery textSearch(String text) {
        QueryNode node = new QueryNode.Or(List.of(
                condition("summary", QueryOperator.CONTAINS, List.of(text)),
                condition("key", QueryOperator.CONTAINS, List.of(text))));
        return new ParsedQuery(node, List.of());
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private static ParsedQuery single(QueryNode node) {
        return new ParsedQuery(node, List.of());
    }

    /**
     * Konum bilgisi 0 verilir: bu koşulların kaynak metinde bir karşılığı yok.
     * Hata üretilirse arayüz onu sorgu editöründe değil, seçim kontrolünde gösterir.
     */
    private static QueryNode.Condition condition(String field, QueryOperator op, List<String> values) {
        List<QueryNode.ValueExpr> literals = values.stream()
                .map(v -> (QueryNode.ValueExpr) new QueryNode.Literal(v, QueryTokenType.STRING, 0, 1))
                .toList();
        return new QueryNode.Condition(field, op, literals, 0, 1);
    }
}
