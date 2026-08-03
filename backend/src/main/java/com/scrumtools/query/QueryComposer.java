package com.scrumtools.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Birden çok STQL parçasını tek bir sorguda birleştirir.
 *
 * Zengin filtre çalışma zamanında sorgu şu şekilde kurulur:
 * <pre>
 *   temel AND (sabit seçimler) AND (dinamik seçimler) AND (akıllı seçimler)
 * </pre>
 * Kontroller arası AND, kontrol içi çoklu seçim OR'dur.
 *
 * Birleştirme <b>metin düzeyinde değil, ağaç düzeyinde</b> yapılır. Parçaları
 * {@code "(" + a + ") AND (" + b + ")"} biçiminde birleştirmek üç şeyi bozardı:
 * {@code ORDER BY} içeren parçalar sözdizimi hatası verir, operatör önceliği
 * parantez hatalarına açılır ve hata konumları ({@code position}) anlamını yitirir.
 *
 * Bkz. RICH_FILTER_PLAN.md — K1, K3.
 */
public final class QueryComposer {

    /**
     * Birleşmiş ağacın azami düğüm sayısı.
     *
     * Yazılan her parça kendi başına 50 koşul sınırına tabidir; birleşme bu sınırı
     * doğal olarak aşar. Bu tavan, çok sayıda seçimin veri tabanını kilitleyecek
     * bir predicate ağacına dönüşmesini engeller.
     */
    public static final int MAX_NODES = 200;

    private QueryComposer() {
    }

    // ─── Birleştirme ──────────────────────────────────────────────────────────

    /**
     * Temel sorguyu, her biri kendi içinde OR'lanan gruplarla AND'ler.
     * Grupların {@code ORDER BY}'ı yok sayılır — sıralama tek kaynaktan gelir.
     */
    public static ParsedQuery compose(ParsedQuery base, List<List<ParsedQuery>> orGroups) {
        return compose(base, orGroups, null);
    }

    /**
     * @param orderOverride sıralamayı devralacak liste (ör. seçili görünüm); boşsa
     *                      temel sorgunun {@code ORDER BY}'ı korunur
     */
    public static ParsedQuery compose(ParsedQuery base,
                                      List<List<ParsedQuery>> orGroups,
                                      List<ParsedQuery.OrderBy> orderOverride) {
        List<QueryNode> conjuncts = new ArrayList<>();
        if (base != null && base.hasWhere()) {
            conjuncts.add(base.where());
        }
        if (orGroups != null) {
            for (List<ParsedQuery> group : orGroups) {
                QueryNode node = anyOf(group);
                if (node != null) conjuncts.add(node);
            }
        }

        QueryNode where = allOf(conjuncts);
        requireWithinBudget(where);

        List<ParsedQuery.OrderBy> order = orderOverride != null && !orderOverride.isEmpty()
                ? orderOverride
                : base != null && base.orderBy() != null ? base.orderBy() : List.of();

        return new ParsedQuery(where, order);
    }

    /** İki sorguyu AND'ler — en sık kullanılan kısayol. */
    public static ParsedQuery and(ParsedQuery base, ParsedQuery extra) {
        return compose(base, List.of(List.of(extra == null ? ParsedQuery.empty() : extra)));
    }

    // ─── Düğüm birleştiriciler ────────────────────────────────────────────────

    /**
     * Tek bir kontrolün seçimleri: parçalardan herhangi biri yeterlidir.
     *
     * Grup boşsa (kullanıcı hiçbir şey seçmemiş) ya da parçalardan biri koşulsuzsa
     * {@code null} döner — koşulsuz bir parça OR'a girseydi grubun tamamını doğru
     * yapar, yani kontrolü etkisiz kılardı. Sonucu aynı olan bu iki durumu ayrı
     * ayrı ele almak yerine ikisi de "kısıt yok" olarak geçilir.
     */
    public static QueryNode anyOf(List<ParsedQuery> parts) {
        if (parts == null || parts.isEmpty()) return null;

        List<QueryNode> nodes = new ArrayList<>();
        for (ParsedQuery part : parts) {
            if (part == null || !part.hasWhere()) return null;
            nodes.add(part.where());
        }
        return orNodes(nodes);
    }

    /** Düğümleri AND'ler; tek düğüm kalırsa sarmalamadan döner. */
    public static QueryNode allOf(List<QueryNode> nodes) {
        List<QueryNode> flat = flatten(nodes, true);
        if (flat.isEmpty()) return null;
        return flat.size() == 1 ? flat.getFirst() : new QueryNode.And(List.copyOf(flat));
    }

    /** Düğümleri OR'lar; tek düğüm kalırsa sarmalamadan döner. */
    public static QueryNode orNodes(List<QueryNode> nodes) {
        List<QueryNode> flat = flatten(nodes, false);
        if (flat.isEmpty()) return null;
        return flat.size() == 1 ? flat.getFirst() : new QueryNode.Or(List.copyOf(flat));
    }

    public static QueryNode not(QueryNode node) {
        return node == null ? null : new QueryNode.Not(node);
    }

    /**
     * Aynı türden iç içe düğümleri tek seviyeye indirir.
     * Yedi akıllı filtrenin OR'u, yedi seviyelik ikili ağaç yerine tek Or düğümü olur;
     * hem düğüm bütçesi (bkz. {@link #MAX_NODES}) hem üretilen SQL sadeleşir.
     */
    private static List<QueryNode> flatten(List<QueryNode> nodes, boolean conjunction) {
        List<QueryNode> out = new ArrayList<>();
        if (nodes == null) return out;

        for (QueryNode node : nodes) {
            switch (node) {
                case null -> {
                }
                case QueryNode.And and when conjunction -> out.addAll(flatten(and.children(), true));
                case QueryNode.Or or when !conjunction -> out.addAll(flatten(or.children(), false));
                default -> out.add(node);
            }
        }
        return out;
    }

    // ─── Bütçe ────────────────────────────────────────────────────────────────

    public static int countNodes(QueryNode node) {
        if (node == null) return 0;
        return switch (node) {
            case QueryNode.And and -> 1 + and.children().stream().mapToInt(QueryComposer::countNodes).sum();
            case QueryNode.Or or -> 1 + or.children().stream().mapToInt(QueryComposer::countNodes).sum();
            case QueryNode.Not not -> 1 + countNodes(not.child());
            case QueryNode.Condition _ -> 1;
        };
    }

    /** Bütçe aşılırsa sorgu çalıştırılmadan reddedilir. */
    public static void requireWithinBudget(QueryNode node) {
        int nodes = countNodes(node);
        if (nodes > MAX_NODES) {
            throw new QueryParseException(
                    "Birleşmiş sorgu çok karmaşık (" + nodes + " düğüm, sınır " + MAX_NODES
                            + "). Seçimleri azaltın ya da zengin filtreyi sadeleştirin.", 0, 1);
        }
    }
}
