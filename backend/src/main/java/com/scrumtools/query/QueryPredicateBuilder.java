package com.scrumtools.query;

import com.scrumtools.entity.Task;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * STQL soyut sözdizim ağacını JPA Criteria {@link Predicate}'ine çevirir.
 *
 * Değerler her zaman parametre olarak bağlanır (Criteria API), bu yüzden
 * SQL enjeksiyon yüzeyi yoktur. Alan adları {@link TaskFieldRegistry} üzerinden
 * doğrulanır; katalogda olmayan bir ad asla SQL'e ulaşmaz.
 */
public final class QueryPredicateBuilder {

    /** Öncelik alanının mantıksal sırası — alfabetik sıralama anlamsız olduğu için. */
    private static final List<String> PRIORITY_ORDER = List.of("Critical", "High", "Medium", "Low");

    /** Akıllı filtrelerin iç içe geçebileceği azami derinlik (RICH_FILTER_PLAN K19). */
    private static final int MAX_SMART_DEPTH = 3;

    private final CriteriaBuilder cb;
    private final Root<Task> root;
    private final AbstractQuery<?> query;
    private final QueryContext ctx;

    /** Aynı ilişkiye ikinci kez join atmamak için — tekrarlı join satır çoğaltır. */
    private final Map<String, Join<?, ?>> joins = new HashMap<>();

    /**
     * İşlenmekte olan akıllı filtre zinciri — döngü tespiti için.
     * {@code smart[A] = "x"} kuralı içeride yine kendini çağırırsa sonsuz özyineleme
     * olurdu; yol üzerindeki her kural burada tutulur.
     */
    private final Deque<String> smartPath = new ArrayDeque<>();

    /**
     * Üretilen koşul sayısı. Akıllı filtreler genişledikçe ağaç büyür; bu sayaç
     * {@link QueryComposer#MAX_NODES} tavanını genişleme sonrasında da uygular.
     */
    private int builtConditions = 0;

    public QueryPredicateBuilder(CriteriaBuilder cb, Root<Task> root, AbstractQuery<?> query, QueryContext ctx) {
        this.cb = cb;
        this.root = root;
        this.query = query;
        this.ctx = ctx;
    }

    // ─── Ağaç gezinme ─────────────────────────────────────────────────────────

    public Predicate build(QueryNode node) {
        return switch (node) {
            case QueryNode.And and -> cb.and(and.children().stream().map(this::build).toArray(Predicate[]::new));
            case QueryNode.Or or -> cb.or(or.children().stream().map(this::build).toArray(Predicate[]::new));
            case QueryNode.Not not -> cb.not(build(not.child()));
            case QueryNode.Condition c -> buildCondition(c);
        };
    }

    /** Takım/proje kapsam kısıtı — kullanıcı sorgusundan bağımsız, her zaman uygulanır. */
    public List<Predicate> scopePredicates() {
        List<Predicate> scope = new ArrayList<>();
        scope.add(cb.equal(root.get("team").get("id"), ctx.teamId()));
        if (ctx.projectId() != null) {
            scope.add(cb.equal(root.get("project").get("id"), ctx.projectId()));
        }
        return scope;
    }

    // ─── Tek koşul ────────────────────────────────────────────────────────────

    private Predicate buildCondition(QueryNode.Condition c) {
        guardNodeBudget(c);

        FieldDescriptor field = TaskFieldRegistry.resolve(c.field())
                .orElseThrow(() -> unknownField(c));

        if (!field.supports(c.operator())) {
            throw new QueryParseException(
                    "'" + field.name() + "' alanı '" + c.operator().symbol() + "' operatörünü desteklemiyor. "
                            + "Desteklenenler: " + symbols(field),
                    c.position(), c.length());
        }

        return switch (field.type()) {
            case STRING, ENUM, USER -> stringPredicate(field, c, stringPath(field));
            case TEXT -> stringPredicate(field, c, root.get(field.path()));
            case CUSTOM_FIELD -> stringPredicate(field, c, customFieldPath(field.path()));
            case NUMBER -> numberPredicate(field, c);
            case DATE, DATETIME -> datePredicate(field, c);
            case COLLECTION -> collectionPredicate(field, c);
            case ENTITY_REF -> entityRefPredicate(field, c);
            case SMART_FILTER -> smartPredicate(field, c);
        };
    }

    /**
     * Genişleme sonrası koşul sayısını sınırlar.
     *
     * Yazılan sorgu 50 koşul sınırına tabidir ama {@code smart[…]} kuralları
     * çözümlenirken ağaç katlanarak büyüyebilir; bu tavan o genişlemeyi de kapsar.
     */
    private void guardNodeBudget(QueryNode.Condition c) {
        if (++builtConditions > QueryComposer.MAX_NODES) {
            throw new QueryParseException(
                    "Sorgu, akıllı filtreler çözümlenince çok karmaşık hâle geliyor "
                            + "(sınır " + QueryComposer.MAX_NODES + " koşul). "
                            + "İç içe geçmiş akıllı filtreleri sadeleştirin.",
                    c.position(), c.length());
        }
    }

    private QueryParseException unknownField(QueryNode.Condition c) {
        String hint = TaskFieldRegistry.suggestSimilar(c.field())
                .map(s -> " Şunu mu demek istediniz: '" + s + "'?")
                .orElse("");
        return new QueryParseException(
                "Bilinmeyen alan: '" + c.field() + "'." + hint, c.position(), c.length());
    }

    private static String symbols(FieldDescriptor field) {
        return field.type().operators().stream().map(QueryOperator::symbol).sorted().reduce((a, b) -> a + ", " + b).orElse("");
    }

    // ─── Metin benzeri alanlar ────────────────────────────────────────────────

    private Expression<String> stringPath(FieldDescriptor field) {
        return root.get(field.path());
    }

    /**
     * PostgreSQL JSONB alanından metin çıkarır: customFields->>'key'.
     * Değerler her zaman metin olarak karşılaştırılır — özel alanların tipi
     * tanım tablosunda tutulsa da JSONB'de tipsiz saklanır.
     */
    private Expression<String> customFieldPath(String key) {
        return cb.function("jsonb_extract_path_text", String.class,
                root.get("customFields"), cb.literal(key));
    }

    private Predicate stringPredicate(FieldDescriptor field, QueryNode.Condition c, Expression<String> path) {
        Expression<String> lowered = cb.lower(path);

        return switch (c.operator()) {
            case EQ -> cb.equal(lowered, lower(single(field, c)));
            // Eşit-değil sorgularında boş alanların da dönmesi beklenir; SQL'in
            // NULL semantiği bunu kendiliğinden yapmadığı için açıkça ekleniyor.
            case NEQ -> cb.or(cb.notEqual(lowered, lower(single(field, c))), cb.isNull(path));
            case CONTAINS -> cb.like(lowered, "%" + escapeLike(lower(single(field, c))) + "%", '\\');
            case NOT_CONTAINS -> cb.or(
                    cb.notLike(lowered, "%" + escapeLike(lower(single(field, c))) + "%", '\\'),
                    cb.isNull(path));
            case IN -> lowered.in(loweredValues(field, c));
            case NOT_IN -> cb.or(cb.not(lowered.in(loweredValues(field, c))), cb.isNull(path));
            case IS_EMPTY -> cb.or(cb.isNull(path), cb.equal(cb.trim(path), ""));
            case IS_NOT_EMPTY -> cb.and(cb.isNotNull(path), cb.notEqual(cb.trim(path), ""));
            default -> throw unsupported(field, c);
        };
    }

    // ─── Sayısal alanlar ──────────────────────────────────────────────────────

    private Predicate numberPredicate(FieldDescriptor field, QueryNode.Condition c) {
        Path<Number> path = root.get(field.path());
        Expression<BigDecimal> num = path.as(BigDecimal.class);

        return switch (c.operator()) {
            case EQ -> cb.equal(num, number(field, c));
            case NEQ -> cb.or(cb.notEqual(num, number(field, c)), cb.isNull(path));
            case GT -> cb.greaterThan(num, number(field, c));
            case GTE -> cb.greaterThanOrEqualTo(num, number(field, c));
            case LT -> cb.lessThan(num, number(field, c));
            case LTE -> cb.lessThanOrEqualTo(num, number(field, c));
            case IN -> num.in(numberValues(field, c));
            case NOT_IN -> cb.or(cb.not(num.in(numberValues(field, c))), cb.isNull(path));
            case IS_EMPTY -> cb.isNull(path);
            case IS_NOT_EMPTY -> cb.isNotNull(path);
            default -> throw unsupported(field, c);
        };
    }

    // ─── Tarih alanları ───────────────────────────────────────────────────────

    private Predicate datePredicate(FieldDescriptor field, QueryNode.Condition c) {
        boolean isDateTime = field.type() == FieldType.DATETIME;
        Path<?> path = root.get(field.path());

        if (c.operator() == QueryOperator.IS_EMPTY) return cb.isNull(path);
        if (c.operator() == QueryOperator.IS_NOT_EMPTY) return cb.isNotNull(path);

        LocalDateTime moment = dateValue(field, c);

        // Zaman damgalı alanlarda "= bugün" gün içindeki tüm kayıtları kapsamalı;
        // yoksa saniye hassasiyeti yüzünden hiçbir şey eşleşmez.
        if (isDateTime) {
            Expression<LocalDateTime> expr = path.as(LocalDateTime.class);
            LocalDate day = moment.toLocalDate();
            return switch (c.operator()) {
                case EQ -> cb.between(expr, day.atStartOfDay(), day.atTime(23, 59, 59, 999_999_999));
                case NEQ -> cb.or(
                        cb.not(cb.between(expr, day.atStartOfDay(), day.atTime(23, 59, 59, 999_999_999))),
                        cb.isNull(path));
                case GT -> cb.greaterThan(expr, moment);
                case GTE -> cb.greaterThanOrEqualTo(expr, moment);
                case LT -> cb.lessThan(expr, moment);
                case LTE -> cb.lessThanOrEqualTo(expr, moment);
                default -> throw unsupported(field, c);
            };
        }

        Expression<LocalDate> expr = path.as(LocalDate.class);
        LocalDate value = moment.toLocalDate();
        return switch (c.operator()) {
            case EQ -> cb.equal(expr, value);
            case NEQ -> cb.or(cb.notEqual(expr, value), cb.isNull(path));
            case GT -> cb.greaterThan(expr, value);
            case GTE -> cb.greaterThanOrEqualTo(expr, value);
            case LT -> cb.lessThan(expr, value);
            case LTE -> cb.lessThanOrEqualTo(expr, value);
            default -> throw unsupported(field, c);
        };
    }

    // ─── Koleksiyon alanları (labels, watchers) ───────────────────────────────

    /**
     * Koleksiyonlar EXISTS alt sorgusuyla sorgulanır. Doğrudan JOIN atmak,
     * birden çok eşleşen etikette aynı görevi tekrar tekrar döndürürdü.
     */
    private Predicate collectionPredicate(FieldDescriptor field, QueryNode.Condition c) {
        Path<Collection<String>> path = root.get(field.path());

        return switch (c.operator()) {
            case IS_EMPTY -> cb.isEmpty(path);
            case IS_NOT_EMPTY -> cb.isNotEmpty(path);
            case EQ -> existsInCollection(field, single(field, c), false);
            case NEQ -> cb.not(existsInCollection(field, single(field, c), false));
            case CONTAINS -> existsInCollection(field, single(field, c), true);
            case NOT_CONTAINS -> cb.not(existsInCollection(field, single(field, c), true));
            case IN -> cb.or(values(field, c).stream()
                    .map(v -> existsInCollection(field, v, false))
                    .toArray(Predicate[]::new));
            case NOT_IN -> cb.not(cb.or(values(field, c).stream()
                    .map(v -> existsInCollection(field, v, false))
                    .toArray(Predicate[]::new)));
            default -> throw unsupported(field, c);
        };
    }

    private Predicate existsInCollection(FieldDescriptor field, String value, boolean partial) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<Task> subRoot = sub.from(Task.class);
        Join<Task, String> element = subRoot.join(field.path());
        sub.select(cb.literal(1));

        Expression<String> lowered = cb.lower(element.as(String.class));
        Predicate match = partial
                ? cb.like(lowered, "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%", '\\')
                : cb.equal(lowered, value.toLowerCase(Locale.ROOT));

        sub.where(cb.equal(subRoot.get("id"), root.get("id")), match);
        return cb.exists(sub);
    }

    // ─── İlişkili entity alanları (sprint, project, release, parent) ──────────

    private Predicate entityRefPredicate(FieldDescriptor field, QueryNode.Condition c) {
        Path<?> ref = root.get(field.path());

        if (c.operator() == QueryOperator.IS_EMPTY) return cb.isNull(ref);
        if (c.operator() == QueryOperator.IS_NOT_EMPTY) return cb.isNotNull(ref);

        List<Predicate> matches = new ArrayList<>();
        for (QueryNode.ValueExpr v : c.values()) {
            if (v instanceof QueryNode.FunctionCall fn) {
                matches.add(sprintFunctionPredicate(field, fn));
            } else {
                matches.add(refValuePredicate(field, ((QueryNode.Literal) v).text()));
            }
        }
        Predicate any = matches.size() == 1 ? matches.get(0) : cb.or(matches.toArray(new Predicate[0]));

        return switch (c.operator()) {
            case EQ, IN -> any;
            // Eşleşmeyenlere ilişkisi hiç olmayan görevler de dâhil edilir.
            case NEQ, NOT_IN -> cb.or(cb.not(any), cb.isNull(ref));
            default -> throw unsupported(field, c);
        };
    }

    private Predicate sprintFunctionPredicate(FieldDescriptor field, QueryNode.FunctionCall fn) {
        if (!QueryFunctions.SPRINT_FUNCTIONS.contains(fn.name().toLowerCase(Locale.ROOT))) {
            throw new QueryParseException(
                    "'" + field.name() + "' alanında '" + fn.name() + "()' fonksiyonu kullanılamaz.",
                    fn.position(), fn.length());
        }
        List<UUID> ids = QueryFunctions.resolveSprints(fn, ctx);
        if (ids.isEmpty()) {
            return cb.disjunction(); // hiçbir sprint yoksa hiçbir görev eşleşmez
        }
        return join(field.path()).get("id").in(ids);
    }

    /** Değer UUID ise id, değilse tanım alanlarından biriyle (ad/key) eşleştirilir. */
    private Predicate refValuePredicate(FieldDescriptor field, String value) {
        try {
            UUID id = UUID.fromString(value.trim());
            return cb.equal(join(field.path()).get("id"), id);
        } catch (IllegalArgumentException ignored) {
            // UUID değil → ada göre eşleştir
        }
        List<String> nameFields = field.refNameField().isEmpty() ? List.of("name") : field.refNameField();
        Join<?, ?> join = join(field.path());
        Predicate[] byName = nameFields.stream()
                .map(nf -> cb.equal(cb.lower(join.get(nf).as(String.class)), value.trim().toLowerCase(Locale.ROOT)))
                .toArray(Predicate[]::new);
        return byName.length == 1 ? byName[0] : cb.or(byName);
    }

    /** LEFT JOIN — ilişkisi olmayan görevlerin NEQ/IS EMPTY sorgularından düşmemesi için. */
    private Join<?, ?> join(String path) {
        return joins.computeIfAbsent(path, p -> root.join(p, JoinType.LEFT));
    }

    // ─── Akıllı filtreler (smart["zengin filtre"]) ────────────────────────────

    /**
     * Sınıflandırma alanı: bir görevin hangi akıllı filtreye düştüğü.
     *
     * Semantik, grafiklerdeki {@code CASE WHEN} ile <b>birebir aynı</b> olmak
     * zorundadır: {@code smart[RF] = "Test"}, "Test kuralını sağlayan ve kendisinden
     * önce gelen hiçbir kuralı sağlamayan" demektir. Aksi hâlde bir halka grafiğin
     * dilimine tıklayıp açılan liste, dilimin sayısıyla uyuşmazdı
     * (bkz. RICH_FILTER_PLAN.md — K4, K18).
     */
    private Predicate smartPredicate(FieldDescriptor field, QueryNode.Condition c) {
        String richFilterName = field.path();
        List<SmartClause> clauses = ctx.smartClauses(richFilterName);

        if (clauses == null) {
            throw new QueryParseException(
                    "Zengin filtre bulunamadı: '" + richFilterName + "'.",
                    c.position(), c.length());
        }

        return switch (c.operator()) {
            // Hiç akıllı filtresi olmayan bir zengin filtrede her görev sınıflandırılmamıştır.
            case IS_EMPTY -> clauses.isEmpty() ? cb.conjunction() : cb.not(anyClauseMatches(clauses));
            case IS_NOT_EMPTY -> clauses.isEmpty() ? cb.disjunction() : anyClauseMatches(clauses);
            case EQ -> bucketPredicate(clauses, single(field, c), richFilterName, c);
            case NEQ -> cb.not(bucketPredicate(clauses, single(field, c), richFilterName, c));
            case IN -> cb.or(bucketPredicates(clauses, field, c, richFilterName));
            case NOT_IN -> cb.not(cb.or(bucketPredicates(clauses, field, c, richFilterName)));
            default -> throw unsupported(field, c);
        };
    }

    private Predicate[] bucketPredicates(List<SmartClause> clauses, FieldDescriptor field,
                                         QueryNode.Condition c, String richFilterName) {
        return values(field, c).stream()
                .map(v -> bucketPredicate(clauses, v, richFilterName, c))
                .toArray(Predicate[]::new);
    }

    /** Görevin, adı verilen akıllı filtrenin kovasına düşmesi. */
    private Predicate bucketPredicate(List<SmartClause> clauses, String clauseName,
                                      String richFilterName, QueryNode.Condition c) {
        int index = -1;
        for (int i = 0; i < clauses.size(); i++) {
            if (clauses.get(i).name() != null && clauses.get(i).name().equalsIgnoreCase(clauseName.trim())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            String available = clauses.stream().map(SmartClause::name).reduce((a, b) -> a + ", " + b).orElse("—");
            throw new QueryParseException(
                    "'" + richFilterName + "' içinde '" + clauseName + "' adlı akıllı filtre yok. "
                            + "Mevcutlar: " + available,
                    c.position(), c.length());
        }

        Predicate match = clausePredicate(clauses.get(index));
        if (index == 0) return match;

        // "İlk eşleşen kazanır": önceki kuralların hiçbirine uymamalı.
        Predicate[] earlier = clauses.subList(0, index).stream()
                .map(this::clausePredicate)
                .toArray(Predicate[]::new);
        return cb.and(match, cb.not(cb.or(earlier)));
    }

    private Predicate anyClauseMatches(List<SmartClause> clauses) {
        return cb.or(clauses.stream().map(this::clausePredicate).toArray(Predicate[]::new));
    }

    /**
     * Tek bir akıllı filtre kuralının predicate'i — döngü ve derinlik korumasıyla.
     * Kuralın sorgusu boşsa her görev eşleşir; koşulsuz kural "her şey" demektir.
     */
    private Predicate clausePredicate(SmartClause clause) {
        String key = clause.pathKey();
        if (smartPath.contains(key)) {
            throw new QueryParseException(
                    "Akıllı filtreler birbirini döngüsel olarak çağırıyor: '" + clause.name() + "'.", 0, 1);
        }
        if (smartPath.size() >= MAX_SMART_DEPTH) {
            throw new QueryParseException(
                    "Akıllı filtreler en fazla " + MAX_SMART_DEPTH + " seviye iç içe geçebilir.", 0, 1);
        }

        smartPath.push(key);
        try {
            QueryNode where = clause.query() == null ? null : clause.query().where();
            return where == null ? cb.conjunction() : build(where);
        } finally {
            smartPath.pop();
        }
    }

    /**
     * Sınıflandırma ifadesi: her görevin düştüğü akıllı filtrenin id'si.
     *
     * {@code CASE WHEN} sırayla değerlendirildiği için "ilk eşleşen kazanır" kuralı
     * doğrudan veri tabanı davranışıdır — grafik gruplaması N ayrı sayım sorgusu
     * yerine tek sorguda çıkar (bkz. RICH_FILTER_PLAN.md — K5).
     */
    /**
     * Tek bir kuralın <b>kendi başına</b> eşleşmesi — sıra gözetmeden.
     *
     * Sınıflandırmanın "ilk eşleşen kazanır" kuralı normalde önceki kuralları da
     * hesaba katar; denetim ekranı ise tam olarak bu farkı görünür kılmak için
     * ham eşleşmeyi sorar: "bu kural 60 göreve uyuyor ama 42'sini alabiliyor,
     * kalanı daha önce gelen kurallara gidiyor" (bkz. RICH_FILTER_PLAN.md — Ö5).
     */
    public Predicate clauseMatchPredicate(SmartClause clause) {
        return clausePredicate(clause);
    }

    public Expression<String> smartBucketExpression(List<SmartClause> clauses, String unclassifiedKey) {
        if (clauses == null || clauses.isEmpty()) {
            return cb.literal(unclassifiedKey);
        }
        CriteriaBuilder.Case<String> bucket = cb.selectCase();
        for (SmartClause clause : clauses) {
            bucket = bucket.when(clausePredicate(clause), clause.id().toString());
        }
        return bucket.otherwise(unclassifiedKey);
    }

    // ─── Sıralama ─────────────────────────────────────────────────────────────

    /** ORDER BY maddelerini Criteria sıralamalarına çevirir. */
    public List<Order> buildOrders(List<ParsedQuery.OrderBy> orderBy) {
        List<Order> orders = new ArrayList<>();
        for (ParsedQuery.OrderBy o : orderBy) {
            FieldDescriptor field = TaskFieldRegistry.resolve(o.field())
                    .orElseThrow(() -> new QueryParseException(
                            "Sıralanamayan alan: '" + o.field() + "'.", o.position(), o.length()));
            Expression<?> expr = orderExpression(field);
            orders.add(o.descending() ? cb.desc(expr) : cb.asc(expr));
        }
        return orders;
    }

    private Expression<?> orderExpression(FieldDescriptor field) {
        // Öncelik alfabetik sıralandığında "Critical" ile "Low" arası anlamsız olur;
        // mantıksal ağırlığa çevrilir (küçük sayı = daha acil).
        if ("priority".equals(field.name())) {
            CriteriaBuilder.Case<Integer> caseExpr = cb.selectCase();
            for (int i = 0; i < PRIORITY_ORDER.size(); i++) {
                caseExpr = caseExpr.when(cb.equal(cb.lower(stringPath(field)),
                        PRIORITY_ORDER.get(i).toLowerCase(Locale.ROOT)), i);
            }
            return caseExpr.otherwise(PRIORITY_ORDER.size());
        }
        if (field.type() == FieldType.ENTITY_REF) {
            List<String> nameFields = field.refNameField();
            return join(field.path()).get(nameFields.isEmpty() ? "name" : nameFields.get(0));
        }
        if (field.type() == FieldType.CUSTOM_FIELD) {
            return customFieldPath(field.path());
        }
        if (field.type() == FieldType.COLLECTION) {
            throw new QueryParseException("'" + field.name() + "' alanına göre sıralama yapılamaz.", 0, 1);
        }
        return root.get(field.path());
    }

    // ─── Değer çözümleme ──────────────────────────────────────────────────────

    private String single(FieldDescriptor field, QueryNode.Condition c) {
        List<String> vals = values(field, c);
        if (vals.isEmpty()) {
            throw new QueryParseException("'" + field.name() + "' için değer bekleniyor.",
                    c.position(), c.length());
        }
        return vals.get(0);
    }

    /** Literal ve fonksiyonları metin değerlere çözer. */
    private List<String> values(FieldDescriptor field, QueryNode.Condition c) {
        List<String> out = new ArrayList<>();
        for (QueryNode.ValueExpr v : c.values()) {
            if (v instanceof QueryNode.FunctionCall fn) {
                if (field.type() == FieldType.USER) {
                    out.add(QueryFunctions.resolveUser(fn, ctx));
                } else {
                    throw new QueryParseException(
                            "'" + field.name() + "' alanında fonksiyon kullanılamaz.",
                            fn.position(), fn.length());
                }
            } else {
                out.add(((QueryNode.Literal) v).text());
            }
        }
        return out;
    }

    private List<String> loweredValues(FieldDescriptor field, QueryNode.Condition c) {
        return values(field, c).stream().map(QueryPredicateBuilder::lower).toList();
    }

    private BigDecimal number(FieldDescriptor field, QueryNode.Condition c) {
        return parseNumber(field, single(field, c), c);
    }

    private List<BigDecimal> numberValues(FieldDescriptor field, QueryNode.Condition c) {
        return values(field, c).stream().map(v -> parseNumber(field, v, c)).toList();
    }

    private BigDecimal parseNumber(FieldDescriptor field, String raw, QueryNode.Condition c) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new QueryParseException(
                    "'" + field.name() + "' sayısal bir alan; '" + raw + "' sayıya çevrilemedi.",
                    c.position(), c.length());
        }
    }

    /** Tarih değeri: fonksiyon, göreli literal (-7d) veya ISO tarih metni. */
    private LocalDateTime dateValue(FieldDescriptor field, QueryNode.Condition c) {
        if (c.values().isEmpty()) {
            throw new QueryParseException("'" + field.name() + "' için tarih değeri bekleniyor.",
                    c.position(), c.length());
        }
        QueryNode.ValueExpr v = c.values().get(0);

        if (v instanceof QueryNode.FunctionCall fn) {
            return QueryFunctions.resolveDateTime(fn, ctx);
        }
        QueryNode.Literal lit = (QueryNode.Literal) v;
        if (lit.tokenType() == QueryTokenType.RELATIVE_DATE) {
            return QueryFunctions.resolveRelative(lit.text(), ctx, lit.position(), lit.length());
        }
        return parseIsoDate(lit, field);
    }

    private LocalDateTime parseIsoDate(QueryNode.Literal lit, FieldDescriptor field) {
        String raw = lit.text().trim();
        try {
            if (raw.contains("T")) return LocalDateTime.parse(raw);
            if (raw.contains(" ")) return LocalDateTime.parse(raw.replace(' ', 'T'));
            return LocalDate.parse(raw).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new QueryParseException(
                    "'" + field.name() + "' bir tarih alanı. Beklenen biçim: \"2026-01-15\" "
                            + "veya göreli değer (-7d, 2w) ya da fonksiyon (startOfWeek()). Verilen: '" + raw + "'",
                    lit.position(), lit.length());
        }
    }

    private QueryParseException unsupported(FieldDescriptor field, QueryNode.Condition c) {
        return new QueryParseException(
                "'" + field.name() + "' alanı '" + c.operator().symbol() + "' operatörüyle kullanılamaz.",
                c.position(), c.length());
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    /** LIKE joker karakterlerini kaçırır — kullanıcının yazdığı % ve _ düz metin sayılır. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
