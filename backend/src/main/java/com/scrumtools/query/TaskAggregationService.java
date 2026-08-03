package com.scrumtools.query;

import com.scrumtools.entity.Task;
import com.scrumtools.service.workflow.TaskStatusCatalog;
import com.scrumtools.service.workflow.TaskStatusService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Bir STQL sorgusunun sonucunu bir alana göre gruplayıp sayar veya toplar —
 * grafik widget'larının veri kaynağı.
 *
 * Sorgu motoru yeniden yazılmaz: {@link QueryPredicateBuilder}'ın ürettiği aynı
 * predicate'e Criteria {@code groupBy} + {@code count}/{@code sum} eklenir.
 *
 * Bkz. DASHBOARD_WIDGET_ROADMAP.md — K4, RICH_FILTER_PLAN.md — §6.
 */
@Service
@RequiredArgsConstructor
public class TaskAggregationService {

    /** Yanıtta dönebilecek azami kova sayısı. */
    public static final int MAX_BUCKETS = 100;
    private static final int DEFAULT_LIMIT = 25;

    /**
     * Veri tabanından çekilecek azami ham grup sayısı. Gruplanabilir alanlarda
     * ayrık değer sayısı doğal olarak küçüktür; bu tavan, özel alan (cf[…]) gibi
     * serbest içerikli bir kaynağın belleği doldurmasına karşı emniyet supabıdır.
     */
    private static final int MAX_RAW_GROUPS = 1000;

    /** Değeri olmayan kayıtların kovası. */
    public static final String EMPTY_KEY = "";
    private static final String OTHER_KEY = "__other__";

    private final EntityManager em;
    private final TaskQueryService taskQueryService;
    private final TaskStatusService taskStatusService;

    // ─── Giriş noktaları ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> aggregate(UUID teamId, UUID projectId, String stql,
                                               String groupBy, String metric, Integer limit) {
        return aggregate(teamId, projectId, QueryParser.parse(stql), groupBy, metric, limit);
    }

    /**
     * @param groupBy gruplanacak alan — {@link FieldType#groupable()} olmalı
     * @param metric  {@code "count"} ya da sayısal bir alan adı (ör. {@code storyPoints})
     * @param limit   dönecek kova sayısı; aşan gruplar "Diğer" kovasında toplanır
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> aggregate(UUID teamId, UUID projectId, ParsedQuery parsed,
                                               String groupBy, String metric, Integer limit) {
        FieldDescriptor group = resolveGroupField(groupBy);
        FieldDescriptor measure = resolveMetricField(metric);

        QueryContext ctx = taskQueryService.buildContext(teamId, projectId);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Task> root = query.from(Task.class);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, query, ctx);
        List<Predicate> predicates = new ArrayList<>(builder.scopePredicates());
        if (parsed.hasWhere()) {
            predicates.add(builder.build(parsed.where()));
        }
        query.where(cb.and(predicates.toArray(new Predicate[0])));

        // Akıllı filtre ekseni: kovalar veriden değil, zengin filtrenin kurallarından
        // gelir. Kural listesi hem CASE ifadesini hem de etiket/renkleri besler.
        List<SmartClause> clauses = group.type() == FieldType.SMART_FILTER
                ? requireClauses(ctx, group)
                : null;

        GroupExpression grouping = clauses != null
                ? new GroupExpression(builder.smartBucketExpression(clauses, EMPTY_KEY), null)
                : groupExpression(cb, root, group);

        Expression<? extends Number> value;
        if (measure == null) {
            value = cb.count(root.get("id"));
        } else {
            value = cb.sum(root.get(measure.path()).as(BigDecimal.class));
        }

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupings = new ArrayList<>();
        selections.add(grouping.key());
        groupings.add(grouping.key());
        if (grouping.label() != null) {
            selections.add(grouping.label());
            groupings.add(grouping.label());
        }
        selections.add(value);

        query.multiselect(selections);
        query.groupBy(groupings);
        // Sıralama SQL'de yapılır: kova sayısı tavanı aşarsa elde kalanların en
        // büyükleri olması, rastgele bir kesit olmasından iyidir.
        query.orderBy(cb.desc(value));

        TypedQuery<Tuple> typed = em.createQuery(query);
        typed.setMaxResults(MAX_RAW_GROUPS);
        List<Tuple> rows = typed.getResultList();

        // Akıllı filtre kovaları yazarın kurduğu sonlu bir kümedir (sırası da onun
        // kararı); veriden gelen kovalar gibi kırpılmaz, boş olanlar da gösterilir.
        if (clauses != null) {
            return smartBuckets(rows, group, clauses);
        }
        return trim(toBuckets(rows, group, grouping.label() != null, teamId, projectId), limit);
    }

    /** {@code groupBy: smart[…]} — zengin filtre çözülemezse sorgu reddedilir. */
    private List<SmartClause> requireClauses(QueryContext ctx, FieldDescriptor group) {
        List<SmartClause> clauses = ctx.smartClauses(group.path());
        if (clauses == null) {
            throw new QueryParseException("Zengin filtre bulunamadı: '" + group.path() + "'.", 0, 1);
        }
        return clauses;
    }

    /**
     * Akıllı filtre kovaları — kural sırasına göre, boş olanlar dâhil.
     *
     * Sıfır sayılı kategoriyi gizlemek grafiği kaydırır: bir hafta "Test" dilimi
     * olan panoda ertesi hafta o dilimin yokluğu, sayının sıfır olduğunu değil
     * kategorinin kaldırıldığını düşündürür.
     */
    private List<Map<String, Object>> smartBuckets(List<Tuple> rows, FieldDescriptor group,
                                                   List<SmartClause> clauses) {
        Map<String, Number> valuesByKey = new LinkedHashMap<>();
        for (Tuple row : rows) {
            Object rawKey = row.get(0);
            Number value = (Number) row.get(row.getElements().size() - 1);
            valuesByKey.put(rawKey == null ? EMPTY_KEY : rawKey.toString(), value == null ? 0 : value);
        }

        String fieldText = StqlRenderer.field(group.name());
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (SmartClause clause : clauses) {
            buckets.add(bucket(
                    clause.id().toString(),
                    clause.name(),
                    valuesByKey.getOrDefault(clause.id().toString(), 0),
                    clause.color(),
                    fieldText + " = " + StqlRenderer.literal(clause.name())));
        }

        // Hiçbir kurala uymayanlar: sessizce yutulmaz, tıklanabilir bir kova olur —
        // çoğu zaman eksik bir akıllı filtreyi ya da veri kalitesi sorununu gösterir.
        Number unclassified = valuesByKey.getOrDefault(EMPTY_KEY, 0);
        if (unclassified.doubleValue() > 0) {
            buckets.add(bucket(EMPTY_KEY, "Sınıflandırılmamış", unclassified,
                    null, fieldText + " IS EMPTY"));
        }
        return buckets;
    }

    // ─── Grup ifadesi ─────────────────────────────────────────────────────────

    /**
     * @param key   gruplama anahtarı (ilişkili alanlarda id)
     * @param label anahtarın okunur karşılığı; ayrı bir sütun gerekmiyorsa null
     */
    private record GroupExpression(Expression<?> key, Expression<String> label) {
    }

    private GroupExpression groupExpression(CriteriaBuilder cb, Root<Task> root, FieldDescriptor field) {
        return switch (field.type()) {
            // İlişkili alanlarda anahtar id'dir: aynı ada sahip iki sprint tek kovada
            // birleşmesin, drill-down linki de id ile kesin çalışsın.
            case ENTITY_REF -> {
                Join<Object, Object> join = root.join(field.path(), JoinType.LEFT);
                String nameField = field.refNameField().isEmpty() ? "name" : field.refNameField().getFirst();
                yield new GroupExpression(join.get("id"), join.get(nameField).as(String.class));
            }
            // Koleksiyonlarda satır çoğalması bilinçlidir: iki etiketli bir görev
            // her iki etiketin de kovasında sayılır.
            case COLLECTION -> new GroupExpression(
                    root.join(field.path(), JoinType.LEFT).as(String.class), null);
            case CUSTOM_FIELD -> new GroupExpression(
                    cb.function("jsonb_extract_path_text", String.class,
                            root.get("customFields"), cb.literal(field.path())), null);
            default -> new GroupExpression(root.get(field.path()).as(String.class), null);
        };
    }

    // ─── Kovalar ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> toBuckets(List<Tuple> rows, FieldDescriptor field,
                                                boolean hasLabelColumn, UUID teamId, UUID projectId) {
        TaskStatusCatalog statusCatalog = "status".equals(field.name())
                ? taskStatusService.getCatalog(teamId, projectId)
                : null;

        List<Map<String, Object>> buckets = new ArrayList<>();
        for (Tuple row : rows) {
            Object rawKey = row.get(0);
            String key = rawKey == null ? EMPTY_KEY : rawKey.toString();
            String label = hasLabelColumn ? (String) row.get(1) : key;
            Number value = (Number) row.get(row.getElements().size() - 1);

            boolean empty = rawKey == null || key.isBlank();
            String color = statusCatalog != null && !empty
                    ? Optional.ofNullable(statusCatalog.find(key)).map(TaskStatusCatalog.StatusView::color).orElse(null)
                    : null;

            buckets.add(bucket(
                    empty ? EMPTY_KEY : key,
                    empty ? "(Boş)" : (label == null || label.isBlank() ? key : label),
                    value == null ? 0 : value,
                    color,
                    drillDown(field, key, empty)));
        }
        return buckets;
    }

    /** Kovayı yalnız o kovaya daraltan STQL parçası — grafikten görev listesine geçiş için. */
    private String drillDown(FieldDescriptor field, String key, boolean empty) {
        String fieldText = StqlRenderer.field(field.name());
        if (empty) return fieldText + " IS EMPTY";
        return fieldText + " = " + StqlRenderer.literal(key);
    }

    /**
     * Kova sayısını sınırlar; artanları tek bir "Diğer" kovasında toplar.
     *
     * Kesmek yerine toplamak bilinçli: 30 etiketli bir projede ilk 25'i gösterip
     * kalanını sessizce düşürmek, grafiğin toplamını gerçek toplamdan küçük
     * gösterirdi.
     */
    private List<Map<String, Object>> trim(List<Map<String, Object>> buckets, Integer limit) {
        int max = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_BUCKETS);
        if (buckets.size() <= max) return buckets;

        List<Map<String, Object>> head = new ArrayList<>(buckets.subList(0, max));
        BigDecimal rest = buckets.subList(max, buckets.size()).stream()
                .map(b -> new BigDecimal(b.get("value").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        head.add(bucket(OTHER_KEY, "Diğer (" + (buckets.size() - max) + ")", rest, null, null));
        return head;
    }

    private static Map<String, Object> bucket(String key, String label, Number value, String color, String filter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("label", label);
        map.put("value", value);
        map.put("color", color);
        map.put("filter", filter);
        return map;
    }

    // ─── Alan çözümleme ───────────────────────────────────────────────────────

    private FieldDescriptor resolveGroupField(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            throw new QueryParseException("Gruplanacak alan belirtilmedi.", 0, 1);
        }
        FieldDescriptor field = TaskFieldRegistry.resolve(groupBy)
                .orElseThrow(() -> new QueryParseException(
                        "Bilinmeyen alan: '" + groupBy + "'."
                                + TaskFieldRegistry.suggestSimilar(groupBy)
                                .map(s -> " Şunu mu demek istediniz: '" + s + "'?").orElse(""),
                        0, groupBy.length()));

        if (!field.type().groupable()) {
            throw new QueryParseException(
                    "'" + field.name() + "' alanına göre gruplama yapılamaz. "
                            + "Gruplanabilir alanlar: durum, öncelik, tür, kullanıcı, etiket, sprint, proje, sürüm.",
                    0, groupBy.length());
        }
        return field;
    }

    /** null / "count" → kayıt sayısı; aksi hâlde toplanacak sayısal alan. */
    private FieldDescriptor resolveMetricField(String metric) {
        if (metric == null || metric.isBlank() || "count".equalsIgnoreCase(metric)) {
            return null;
        }
        FieldDescriptor field = TaskFieldRegistry.resolve(metric)
                .orElseThrow(() -> new QueryParseException("Bilinmeyen ölçü alanı: '" + metric + "'.",
                        0, metric.length()));

        if (!field.type().summable()) {
            throw new QueryParseException(
                    "'" + field.name() + "' sayısal bir alan değil; ölçü olarak kullanılamaz. "
                            + "Sayısal alanlar: storyPoints, estimatedHours, loggedHours.",
                    0, metric.length());
        }
        return field;
    }
}
