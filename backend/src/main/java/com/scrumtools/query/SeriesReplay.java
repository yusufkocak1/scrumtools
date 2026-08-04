package com.scrumtools.query;

import java.util.*;

/**
 * Geçmişi yeniden kurgulayan <b>dar</b> sorgu değerlendiricisi — zaman serisi
 * backfill'inin motoru (bkz. RICH_FILTER_PLAN.md — K13).
 *
 * <h3>Neden ikinci bir değerlendirici var?</h3>
 * Asıl motor ({@link QueryPredicateBuilder}) koşulları veri tabanına çevirir ve
 * yalnız <b>bugünkü</b> satırları görebilir. "3 ay önce kaç görev Test'teydi"
 * sorusu ise geçmişteki satırı ister; o satır hiçbir yerde durmuyor, yalnız
 * {@code task_history} üzerindeki değişiklikler ters uygulanarak kurgulanabiliyor.
 * Kurgulanan kayıt bellekte olduğu için SQL'e gönderilemez.
 *
 * <h3>Neden bu kadar dar?</h3>
 * İki farklı semantik, zamanla birbirinden ayrışır ve "grafik şunu, liste bunu
 * diyor" durumunu doğurur (bkz. §13/9). Bu yüzden burası bilinçli olarak küçük
 * tutuldu: yalnız {@link #REPLAYABLE_FIELDS} ve {@link #STABLE_FIELDS} alanları,
 * yalnız metin operatörleri. Kapsam dışındaki her şey "kurgulanamaz" olarak
 * işaretlenir ve seri geçmişsiz, bugünden itibaren birikerek başlar. Sessizce
 * yanlış bir geçmiş üretmek, geçmişi hiç üretmemekten kötüdür.
 *
 * Ayrıca çağıran taraf, kurgunun bugünkü değerini SQL'in bugünkü değeriyle
 * karşılaştırır; tutmuyorsa backfill hiç yazılmaz. Semantik kayması böylece
 * yanlış veriye değil, eksik veriye dönüşür.
 */
public final class SeriesReplay {

    /**
     * {@code task_history} tarafından izlenen ve geriye doğru kurgulanabilen alanlar:
     * STQL alan adı → tarihçedeki alan adı.
     *
     * Liste {@code AuditService.recordChange} çağrılarından türetildi. İzlenmeyen bir
     * alanı "hiç değişmemiş" saymak, örneğin dün çözülmüş bir görevi 180 gün boyunca
     * çözülmüş göstermek olurdu; o yüzden izlenmeyen her alan kapsam dışıdır.
     */
    public static final Map<String, String> REPLAYABLE_FIELDS = Map.of(
            "status", "status",
            "priority", "priority",
            "assignee", "assignee",
            "summary", "title");

    /**
     * Hiç değişmeyen alanlar — bugünkü değerleri geçmişte de geçerlidir.
     * Görev anahtarı proje değişse bile korunuyor ({@code TaskService}), açan kişi
     * ise yalnız oluşturulurken yazılıyor.
     */
    public static final Set<String> STABLE_FIELDS = Set.of("key", "reporter");

    /** Metin alanlarının operatörleri; sayısal/tarihsel karşılaştırma kapsam dışı. */
    private static final Set<QueryOperator> SUPPORTED_OPERATORS = EnumSet.of(
            QueryOperator.EQ, QueryOperator.NEQ,
            QueryOperator.CONTAINS, QueryOperator.NOT_CONTAINS,
            QueryOperator.IN, QueryOperator.NOT_IN,
            QueryOperator.IS_EMPTY, QueryOperator.IS_NOT_EMPTY);

    private SeriesReplay() {
    }

    /**
     * Kurgulanabilirlik sonucu.
     *
     * @param supported  sorgu geriye doğru kurgulanabilir mi
     * @param reason     kurgulanamıyorsa kullanıcıya gösterilecek sebep
     * @param replayed   tarihçeden kurgulanacak alanların kanonik adları
     */
    public record Support(boolean supported, String reason, Set<String> replayed) {

        static Support ok(Set<String> replayed) {
            return new Support(true, null, Set.copyOf(replayed));
        }

        static Support no(String reason) {
            return new Support(false, reason, Set.of());
        }
    }

    // ─── Çözümleme ────────────────────────────────────────────────────────────

    /** Sorgunun geriye doğru kurgulanıp kurgulanamayacağını söyler. */
    public static Support analyze(QueryNode node) {
        Set<String> replayed = new LinkedHashSet<>();
        String reason = walk(node, replayed);
        return reason == null ? Support.ok(replayed) : Support.no(reason);
    }

    /** @return kurgulanamama sebebi; kurgulanabiliyorsa null */
    private static String walk(QueryNode node, Set<String> replayed) {
        if (node == null) return null;

        return switch (node) {
            case QueryNode.And and -> firstProblem(and.children(), replayed);
            case QueryNode.Or or -> firstProblem(or.children(), replayed);
            case QueryNode.Not not -> walk(not.child(), replayed);
            case QueryNode.Condition c -> conditionProblem(c, replayed);
        };
    }

    private static String firstProblem(List<QueryNode> children, Set<String> replayed) {
        for (QueryNode child : children) {
            String problem = walk(child, replayed);
            if (problem != null) return problem;
        }
        return null;
    }

    private static String conditionProblem(QueryNode.Condition c, Set<String> replayed) {
        FieldDescriptor field = TaskFieldRegistry.resolve(c.field()).orElse(null);
        if (field == null) {
            return "Bilinmeyen alan: '" + c.field() + "'.";
        }

        String name = field.name();
        boolean replayable = REPLAYABLE_FIELDS.containsKey(name);
        if (!replayable && !STABLE_FIELDS.contains(name)) {
            return "'" + name + "' alanı geçmişe dönük izlenmiyor; bu sorgunun geçmişi kurgulanamaz. "
                    + "Kurgulanabilen alanlar: " + String.join(", ", sortedSupportedFields()) + ".";
        }
        if (!SUPPORTED_OPERATORS.contains(c.operator())) {
            return "'" + name + " " + c.operator().symbol()
                    + "' karşılaştırması geçmiş kurgusunda desteklenmiyor.";
        }
        for (QueryNode.ValueExpr value : c.values()) {
            if (value instanceof QueryNode.FunctionCall fn && field.type() != FieldType.USER) {
                return "'" + fn.name() + "()' fonksiyonu geçmiş kurgusunda kullanılamaz.";
            }
        }

        if (replayable) replayed.add(name);
        return null;
    }

    private static List<String> sortedSupportedFields() {
        List<String> all = new ArrayList<>(REPLAYABLE_FIELDS.keySet());
        all.addAll(STABLE_FIELDS);
        Collections.sort(all);
        return all;
    }

    // ─── Değerlendirme ────────────────────────────────────────────────────────

    /**
     * Kurgulanmış bir görev anlık görüntüsünün sorguyla eşleşip eşleşmediği.
     *
     * @param snapshot kanonik alan adı → o andaki değer (null olabilir)
     * @param ctx      {@code currentUser()} gibi fonksiyonların çözüm bağlamı
     */
    public static boolean matches(QueryNode node, Map<String, String> snapshot, QueryContext ctx) {
        if (node == null) return true;

        return switch (node) {
            case QueryNode.And and -> and.children().stream().allMatch(n -> matches(n, snapshot, ctx));
            case QueryNode.Or or -> or.children().stream().anyMatch(n -> matches(n, snapshot, ctx));
            case QueryNode.Not not -> !matches(not.child(), snapshot, ctx);
            case QueryNode.Condition c -> conditionMatches(c, snapshot, ctx);
        };
    }

    private static boolean conditionMatches(QueryNode.Condition c, Map<String, String> snapshot,
                                            QueryContext ctx) {
        FieldDescriptor field = TaskFieldRegistry.resolve(c.field())
                .orElseThrow(() -> new QueryParseException(
                        "Bilinmeyen alan: '" + c.field() + "'.", c.position(), c.length()));

        String actual = snapshot.get(field.name());
        List<String> values = literals(field, c, ctx);

        // Semantik, QueryPredicateBuilder.stringPredicate ile birebir aynı olmalı:
        // NULL'lu satırların NEQ/NOT_IN sorgularına dâhil olması dâhil.
        return switch (c.operator()) {
            case EQ -> actual != null && equalsIgnoreCase(actual, first(values));
            case NEQ -> actual == null || !equalsIgnoreCase(actual, first(values));
            case CONTAINS -> actual != null && lower(actual).contains(lower(first(values)));
            case NOT_CONTAINS -> actual == null || !lower(actual).contains(lower(first(values)));
            case IN -> actual != null && containsIgnoreCase(values, actual);
            case NOT_IN -> actual == null || !containsIgnoreCase(values, actual);
            case IS_EMPTY -> isBlank(actual);
            case IS_NOT_EMPTY -> !isBlank(actual);
            default -> throw new QueryParseException(
                    "'" + field.name() + "' alanı '" + c.operator().symbol()
                            + "' operatörüyle geçmiş kurgusunda kullanılamaz.",
                    c.position(), c.length());
        };
    }

    /** Değer listesi — {@code QueryPredicateBuilder.values} ile aynı çözüm. */
    private static List<String> literals(FieldDescriptor field, QueryNode.Condition c, QueryContext ctx) {
        List<String> out = new ArrayList<>();
        for (QueryNode.ValueExpr v : c.values()) {
            if (v instanceof QueryNode.FunctionCall fn) {
                out.add(QueryFunctions.resolveUser(fn, ctx));
            } else {
                out.add(((QueryNode.Literal) v).text());
            }
        }
        return out;
    }

    private static String first(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return lower(a).equals(lower(b));
    }

    private static boolean containsIgnoreCase(List<String> values, String actual) {
        String needle = lower(actual);
        return values.stream().anyMatch(v -> lower(v).equals(needle));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
