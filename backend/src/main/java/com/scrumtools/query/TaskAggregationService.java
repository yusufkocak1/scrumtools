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
import java.util.stream.Collectors;

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

    // ─── İki boyutlu gruplama (ısı haritası) ──────────────────────────────────

    /** Isı haritasında bir eksende gösterilecek azami kategori. */
    private static final int MAX_AXIS_ROWS = 25;
    private static final int MAX_AXIS_COLUMNS = 15;

    /**
     * İki alana göre gruplama — {@code atanan × öncelik} gibi bir matris.
     *
     * Tek sorguda çıkar: iki gruplama ifadesi yan yana konur, hücreler sonuçtan
     * toplanır. N×M ayrı sayım sorgusu atmak, 10×5'lik bir matriste 50 gidiş-dönüş
     * demek olurdu.
     *
     * Eksenler kırpılır (satırda {@value #MAX_AXIS_ROWS}, sütunda
     * {@value #MAX_AXIS_COLUMNS}); kırpma olduysa {@code truncated} ile bildirilir.
     * Burada "Diğer" kovası yok: matriste artıkları tek bir satırda toplamak,
     * kesişimleri anlamsız kılardı.
     *
     * @return {@code {rows, columns, cells, max, truncated}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> matrix(UUID teamId, UUID projectId, ParsedQuery parsed,
                                      String rowField, String columnField, String metric) {
        FieldDescriptor rowDescriptor = resolveGroupField(rowField);
        FieldDescriptor columnDescriptor = resolveGroupField(columnField);
        FieldDescriptor measure = resolveMetricField(metric);

        QueryContext ctx = taskQueryService.buildContext(teamId, projectId);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Task> root = query.from(Task.class);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, query, ctx);
        List<Predicate> predicates = new ArrayList<>(builder.scopePredicates());
        if (parsed.hasWhere()) predicates.add(builder.build(parsed.where()));
        query.where(cb.and(predicates.toArray(new Predicate[0])));

        Axis rowAxis = axis(cb, root, builder, ctx, rowDescriptor);
        Axis columnAxis = axis(cb, root, builder, ctx, columnDescriptor);

        Expression<? extends Number> value = measure == null
                ? cb.count(root.get("id"))
                : cb.sum(root.get(measure.path()).as(BigDecimal.class));

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupings = new ArrayList<>();
        int columnKeyIndex = addAxis(selections, groupings, rowAxis);
        int valueIndex = addAxis(selections, groupings, columnAxis);
        selections.add(value);

        query.multiselect(selections);
        query.groupBy(groupings);

        TypedQuery<Tuple> typed = em.createQuery(query);
        typed.setMaxResults(MAX_RAW_GROUPS);
        return assemble(typed.getResultList(), rowAxis, columnAxis, columnKeyIndex, valueIndex);
    }

    /**
     * Bir eksenin gruplama ifadesi ve kategori sırası.
     *
     * @param clauses akıllı filtre ekseninde kural listesi; alan ekseninde null
     */
    private record Axis(FieldDescriptor field, GroupExpression expression, List<SmartClause> clauses) {

        boolean hasLabel() {
            return expression.label() != null;
        }
    }

    /** Matris hücresinin adresi. */
    private record CellKey(String row, String column) {
    }

    private Axis axis(CriteriaBuilder cb, Root<Task> root, QueryPredicateBuilder builder,
                      QueryContext ctx, FieldDescriptor field) {
        if (field.type() == FieldType.SMART_FILTER) {
            List<SmartClause> clauses = requireClauses(ctx, field);
            return new Axis(field, new GroupExpression(
                    builder.smartBucketExpression(clauses, EMPTY_KEY), null), clauses);
        }
        return new Axis(field, groupExpression(cb, root, field), null);
    }

    /** @return eklenen son seçimin bir sonraki indeksi */
    private static int addAxis(List<Selection<?>> selections, List<Expression<?>> groupings, Axis axis) {
        selections.add(axis.expression().key());
        groupings.add(axis.expression().key());
        if (axis.hasLabel()) {
            selections.add(axis.expression().label());
            groupings.add(axis.expression().label());
        }
        return selections.size();
    }

    private Map<String, Object> assemble(List<Tuple> rows, Axis rowAxis, Axis columnAxis,
                                         int columnKeyIndex, int valueIndex) {
        Map<String, String> rowLabels = new LinkedHashMap<>();
        Map<String, String> columnLabels = new LinkedHashMap<>();
        Map<String, BigDecimal> rowTotals = new HashMap<>();
        Map<String, BigDecimal> columnTotals = new HashMap<>();
        // Anahtarlar kullanıcı verisi (durum adı, e-posta, etiket); ikisini bir
        // ayraçla birleştirmek "To Do" gibi değerlerde çakışırdı.
        Map<CellKey, BigDecimal> cells = new LinkedHashMap<>();

        for (Tuple row : rows) {
            String rowKey = keyOf(row.get(0));
            String columnKey = keyOf(row.get(columnKeyIndex));
            BigDecimal value = decimal(row.get(valueIndex));

            rowLabels.putIfAbsent(rowKey, labelOf(row, rowAxis, 1, rowKey));
            columnLabels.putIfAbsent(columnKey, labelOf(row, columnAxis, columnKeyIndex + 1, columnKey));

            rowTotals.merge(rowKey, value, BigDecimal::add);
            columnTotals.merge(columnKey, value, BigDecimal::add);
            cells.merge(new CellKey(rowKey, columnKey), value, BigDecimal::add);
        }

        List<Map<String, Object>> orderedRows = orderAxis(rowAxis, rowLabels, rowTotals, MAX_AXIS_ROWS);
        List<Map<String, Object>> orderedColumns = orderAxis(columnAxis, columnLabels, columnTotals, MAX_AXIS_COLUMNS);

        Set<String> keptRows = orderedRows.stream().map(r -> (String) r.get("key")).collect(Collectors.toSet());
        Set<String> keptColumns = orderedColumns.stream().map(c -> (String) c.get("key")).collect(Collectors.toSet());

        List<Map<String, Object>> cellList = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        for (Map.Entry<CellKey, BigDecimal> entry : cells.entrySet()) {
            CellKey key = entry.getKey();
            if (!keptRows.contains(key.row()) || !keptColumns.contains(key.column())) continue;

            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("row", key.row());
            cell.put("column", key.column());
            cell.put("value", entry.getValue());
            cellList.add(cell);
            if (entry.getValue().compareTo(max) > 0) max = entry.getValue();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", orderedRows);
        out.put("columns", orderedColumns);
        out.put("cells", cellList);
        out.put("max", max);
        out.put("truncated", rowLabels.size() > orderedRows.size() || columnLabels.size() > orderedColumns.size());
        return out;
    }

    /**
     * Eksenin kategori sırası.
     *
     * Akıllı filtre ekseninde sıra yazarın kararıdır ve boş kategoriler de kalır;
     * alan ekseninde en kalabalık kategoriler öne alınır (bkz. §13/8).
     */
    private List<Map<String, Object>> orderAxis(Axis axis, Map<String, String> labels,
                                                Map<String, BigDecimal> totals, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();

        if (axis.clauses() != null) {
            for (SmartClause clause : axis.clauses()) {
                out.add(axisEntry(clause.id().toString(), clause.name(), clause.color()));
            }
            if (totals.containsKey(EMPTY_KEY)) {
                out.add(axisEntry(EMPTY_KEY, "Sınıflandırılmamış", null));
            }
            return out;
        }

        labels.entrySet().stream()
                .sorted(Comparator.comparing(
                        (Map.Entry<String, String> e) -> totals.getOrDefault(e.getKey(), BigDecimal.ZERO)).reversed())
                .limit(limit)
                .forEach(e -> out.add(axisEntry(e.getKey(), e.getValue(), null)));
        return out;
    }

    private static Map<String, Object> axisEntry(String key, String label, String color) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("label", label == null || label.isBlank() ? "(Boş)" : label);
        entry.put("color", color);
        return entry;
    }

    private static String keyOf(Object raw) {
        return raw == null ? EMPTY_KEY : raw.toString();
    }

    private static String labelOf(Tuple row, Axis axis, int labelIndex, String fallback) {
        if (!axis.hasLabel()) return fallback;
        Object label = row.get(labelIndex);
        return label == null ? fallback : label.toString();
    }

    private static BigDecimal decimal(Object raw) {
        if (raw == null) return BigDecimal.ZERO;
        return raw instanceof BigDecimal big ? big : new BigDecimal(raw.toString());
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
