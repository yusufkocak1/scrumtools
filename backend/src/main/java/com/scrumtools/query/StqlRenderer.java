package com.scrumtools.query;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Soyut sözdizim ağacını yeniden STQL metnine yazar — {@link QueryParser}'ın tersi.
 *
 * Birleştirme ağaç düzeyinde yapıldığı için (bkz. {@link QueryComposer}) bileşik
 * sorgunun metin karşılığı ortada yoktur. Bu sınıf onu üretir; kazancı grafikten
 * göreve giden köprüdür: bir grafik diliminden "Görevlerde aç" denince kullanıcı
 * mevcut görev listesi ekranında ({@code ?q=}) tam olarak o kümeyi görür, ayrı bir
 * drill-down ekranı yazmak gerekmez.
 *
 * Üretilen metin yeniden çözümlendiğinde aynı ağacı vermelidir; bu yüzden değerler
 * tereddütte kalınırsa tırnaklanır.
 *
 * Bkz. RICH_FILTER_PLAN.md — K8.
 */
public final class StqlRenderer {

    /** Tırnaksız yazıldığında anahtar kelimeyle karışacak değerler. */
    private static final Set<String> RESERVED = Set.of(
            "and", "or", "not", "in", "is", "empty", "null", "order", "by", "asc", "desc");

    private StqlRenderer() {
    }

    /** Tam sorgu: koşullar + ORDER BY. */
    public static String render(ParsedQuery query) {
        if (query == null) return "";

        StringBuilder sb = new StringBuilder();
        if (query.hasWhere()) {
            sb.append(render(query.where()));
        }
        if (query.hasOrderBy()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append("ORDER BY ").append(query.orderBy().stream()
                    .map(o -> o.field() + (o.descending() ? " DESC" : ""))
                    .collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }

    /** Yalnız koşul ağacı. */
    public static String render(QueryNode node) {
        return node == null ? "" : write(node, 0);
    }

    // ─── Ağaç gezinme ─────────────────────────────────────────────────────────

    /**
     * @param minPrecedence üst düğümün gerektirdiği asgari öncelik; düğümün önceliği
     *                      bunun altındaysa parantezlenir
     */
    private static String write(QueryNode node, int minPrecedence) {
        return switch (node) {
            case QueryNode.Or or -> parenthesize(
                    join(or.children(), " OR ", PREC_OR), PREC_OR, minPrecedence);
            case QueryNode.And and -> parenthesize(
                    join(and.children(), " AND ", PREC_AND), PREC_AND, minPrecedence);
            // NOT'un çocuğu bileşikse her hâlükârda parantez ister: "NOT a AND b"
            // çözümlendiğinde "(NOT a) AND b" olurdu.
            case QueryNode.Not not -> "NOT " + write(not.child(), PREC_NOT);
            case QueryNode.Condition c -> condition(c);
        };
    }

    private static final int PREC_OR = 1;
    private static final int PREC_AND = 2;
    private static final int PREC_NOT = 3;

    private static String join(List<QueryNode> children, String separator, int precedence) {
        return children.stream()
                .map(child -> write(child, precedence))
                .collect(Collectors.joining(separator));
    }

    private static String parenthesize(String text, int precedence, int minPrecedence) {
        return precedence < minPrecedence ? "(" + text + ")" : text;
    }

    // ─── Tek koşul ────────────────────────────────────────────────────────────

    private static String condition(QueryNode.Condition c) {
        String field = field(c.field());
        QueryOperator op = c.operator();

        if (op.isUnary()) {
            return field + " " + op.symbol();
        }
        if (op.isMultiValue()) {
            return field + " " + op.symbol() + " ("
                    + c.values().stream().map(StqlRenderer::value).collect(Collectors.joining(", "))
                    + ")";
        }
        String rhs = c.values().isEmpty() ? "\"\"" : value(c.values().getFirst());
        return field + " " + op.symbol() + " " + rhs;
    }

    private static String value(QueryNode.ValueExpr expr) {
        return switch (expr) {
            case QueryNode.FunctionCall fn -> fn.name() + "("
                    + fn.args().stream().map(StqlRenderer::literal).collect(Collectors.joining(", "))
                    + ")";
            // Sayılar ve göreli tarihler tırnaklanmaz: "-7d" tırnak içinde düz metne
            // dönüşür ve tarih olarak yeniden çözümlenmez.
            case QueryNode.Literal lit when lit.tokenType() == QueryTokenType.NUMBER
                    || lit.tokenType() == QueryTokenType.RELATIVE_DATE -> lit.text();
            case QueryNode.Literal lit -> literal(lit.text());
        };
    }

    /**
     * Alan adını yazılabilir hâle getirir.
     *
     * Köşeli parantezli alanlarda anahtar, çözümleme sırasında tırnaklarından
     * arındırılmıştır: {@code smart["Sprint Sağlığı"]} ağaçta {@code smart[Sprint Sağlığı]}
     * olarak durur. Olduğu gibi yazılırsa yeniden çözümlenemez — anahtar burada
     * tekrar tırnaklanır.
     */
    public static String field(String raw) {
        if (raw == null) return "";
        int open = raw.indexOf('[');
        if (open <= 0 || !raw.endsWith("]")) return raw;
        return raw.substring(0, open) + "[" + literal(raw.substring(open + 1, raw.length() - 1)) + "]";
    }

    /**
     * Değeri gerekiyorsa tırnaklar.
     *
     * Ölçüt bilinçli olarak dar: yalnız ASCII harf/rakam/alt çizgiden oluşan ve
     * anahtar kelime olmayan değerler tırnaksız bırakılır. Türkçe karakterli veya
     * boşluklu değerlerin tırnaksız kalması lexer'ın tanımlayıcı kurallarına
     * bağımlılık yaratırdı; fazladan tırnak zararsızdır, eksik tırnak sorguyu bozar.
     */
    public static String literal(String raw) {
        if (raw == null) return "\"\"";
        boolean bare = !raw.isEmpty()
                && raw.chars().allMatch(ch -> ch == '_'
                        || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9'))
                && !RESERVED.contains(raw.toLowerCase(Locale.ROOT));

        return bare ? raw : '"' + raw.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
