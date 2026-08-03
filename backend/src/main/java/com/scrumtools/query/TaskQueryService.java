package com.scrumtools.query;

import com.scrumtools.dto.TaskResponse;
import com.scrumtools.entity.Sprint;
import com.scrumtools.entity.Task;
import com.scrumtools.repository.SprintRepository;
import com.scrumtools.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * STQL sorgularını çalıştıran servis.
 *
 * Hem yeni sorgu dili ucu hem de eski {@code filters[]} tabanlı
 * {@link com.scrumtools.service.TaskFilterService} bu motoru kullanır — böylece
 * iki ayrı filtreleme kod yolu oluşmaz.
 */
@Service
@RequiredArgsConstructor
public class TaskQueryService {

    /** Tek sayfada dönebilecek azami kayıt. */
    public static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final EntityManager em;
    private final TeamRepository teamRepository;
    private final SprintRepository sprintRepository;
    private final SmartFilterCatalog smartFilterCatalog;

    // ─── Genel giriş noktaları ────────────────────────────────────────────────

    /** STQL metnini çözümleyip çalıştırır. */
    @Transactional(readOnly = true)
    public Map<String, Object> search(UUID teamId, UUID projectId, String stql,
                                      Integer page, Integer size) {
        ParsedQuery parsed = QueryParser.parse(stql);
        return execute(teamId, projectId, parsed, null, null, page, size);
    }

    /**
     * Çözümlenmiş sorguyu çalıştırır.
     *
     * @param fallbackSortBy  sorguda ORDER BY yoksa kullanılacak alan
     * @param fallbackSortDir "asc" | "desc"
     */
    @Transactional(readOnly = true)
    public Map<String, Object> execute(UUID teamId, UUID projectId, ParsedQuery parsed,
                                       String fallbackSortBy, String fallbackSortDir,
                                       Integer page, Integer size) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        QueryContext ctx = buildContext(teamId, projectId);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        long total = countMatching(cb, ctx, parsed);

        CriteriaQuery<Task> dataQuery = cb.createQuery(Task.class);
        Root<Task> root = dataQuery.from(Task.class);
        dataQuery.select(root);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, dataQuery, ctx);
        dataQuery.where(cb.and(allPredicates(builder, parsed).toArray(new Predicate[0])));
        dataQuery.orderBy(resolveOrders(cb, root, builder, parsed, fallbackSortBy, fallbackSortDir));

        int pageIndex = page != null && page > 0 ? page : 0;
        int pageSize = normalizeSize(size);

        TypedQuery<Task> typed = em.createQuery(dataQuery);
        typed.setFirstResult(pageIndex * pageSize);
        typed.setMaxResults(pageSize);

        List<TaskResponse> content = typed.getResultList().stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));
        result.put("page", pageIndex);
        result.put("size", pageSize);
        return result;
    }

    /**
     * Sorguyu doğrular ve geçerliyse eşleşen kayıt sayısını da döner.
     *
     * Editör yazarken bunu çağırır; doğrulama ve sayaç tek istekte birleştirilmiştir —
     * ayrı uçlar her tuş vuruşunda iki gidiş-dönüş demek olurdu. Sözdizimi hatası
     * bir HTTP hatası değil veri olarak döner: yazım hâlindeki sorgu "hata" değildir.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validate(UUID teamId, UUID projectId, String stql) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            ParsedQuery parsed = QueryParser.parse(stql);
            // Alan/operatör uyumu ancak Criteria'ya çevrilirken anlaşılır; sorguyu
            // çalıştırmadan derleyerek anlamsal hataları da yakalıyoruz.
            compileOnly(teamId, projectId, parsed);

            out.put("valid", true);
            out.put("conditionCount", countConditions(parsed.where()));
            out.put("count", countMatching(em.getCriteriaBuilder(),
                    buildContext(teamId, projectId), parsed));
        } catch (QueryParseException e) {
            out.put("valid", false);
            out.put("error", Map.of(
                    "message", e.getMessage(),
                    "position", e.getPosition(),
                    "length", e.getLength()));
        }
        return out;
    }

    /** Sorguyu Criteria'ya çevirip atar — anlamsal doğrulama için. */
    private void compileOnly(UUID teamId, UUID projectId, ParsedQuery parsed) {
        QueryContext ctx = buildContext(teamId, projectId);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Task> q = cb.createQuery(Task.class);
        Root<Task> root = q.from(Task.class);
        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, q, ctx);
        allPredicates(builder, parsed);
        builder.buildOrders(parsed.orderBy());
    }

    /** Bir sorgunun eşleşen kayıt sayısı — dashboard sayaç widget'ları için de kullanılabilir. */
    @Transactional(readOnly = true)
    public long count(UUID teamId, UUID projectId, String stql) {
        QueryContext ctx = buildContext(teamId, projectId);
        return countMatching(em.getCriteriaBuilder(), ctx, QueryParser.parse(stql));
    }

    // ─── Bağlam ───────────────────────────────────────────────────────────────

    public QueryContext buildContext(UUID teamId, UUID projectId) {
        // Aynı sorguda birden çok smart[…] koşulu aynı zengin filtreye bakabilir;
        // çözüm bağlam ömrü boyunca hatırlanır, her koşul için tekrar sorulmaz.
        Map<String, List<SmartClause>> smartCache = new HashMap<>();

        return new QueryContext(
                teamId,
                projectId,
                currentUserEmail(),
                (tid, status) -> sprintRepository.findByTeamIdAndStatus(tid, status)
                        .stream().map(Sprint::getId).toList(),
                (tid, name) -> smartCache.computeIfAbsent(
                        name == null ? "" : name.toLowerCase(Locale.ROOT),
                        _ -> smartFilterCatalog.clausesOf(tid, name)),
                LocalDateTime.now());
    }

    private static String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private long countMatching(CriteriaBuilder cb, QueryContext ctx, ParsedQuery parsed) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> countRoot = countQuery.from(Task.class);
        countQuery.select(cb.count(countRoot));

        QueryPredicateBuilder countBuilder = new QueryPredicateBuilder(cb, countRoot, countQuery, ctx);
        countQuery.where(cb.and(allPredicates(countBuilder, parsed).toArray(new Predicate[0])));
        return em.createQuery(countQuery).getSingleResult();
    }

    /** Kapsam kısıtları + kullanıcı koşulu. Kapsam her zaman AND'lenir. */
    private List<Predicate> allPredicates(QueryPredicateBuilder builder, ParsedQuery parsed) {
        List<Predicate> preds = new ArrayList<>(builder.scopePredicates());
        if (parsed.hasWhere()) {
            preds.add(builder.build(parsed.where()));
        }
        return preds;
    }

    private List<Order> resolveOrders(CriteriaBuilder cb, Root<Task> root, QueryPredicateBuilder builder,
                                      ParsedQuery parsed, String fallbackSortBy, String fallbackSortDir) {
        if (parsed.hasOrderBy()) {
            return builder.buildOrders(parsed.orderBy());
        }
        // Sorguda ORDER BY yoksa çağıranın (ör. eski filters[] ucu) sıralaması geçerli.
        String field = fallbackSortBy != null && !fallbackSortBy.isBlank() ? fallbackSortBy : "createdAt";
        FieldDescriptor descriptor = TaskFieldRegistry.resolve(field).orElse(null);
        String path = descriptor != null ? descriptor.path() : "createdAt";
        Path<?> expr;
        try {
            expr = root.get(path);
        } catch (IllegalArgumentException e) {
            expr = root.get("createdAt");
        }
        return List.of("asc".equalsIgnoreCase(fallbackSortDir) ? cb.asc(expr) : cb.desc(expr));
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static int countConditions(QueryNode node) {
        if (node == null) return 0;
        return switch (node) {
            case QueryNode.And and -> and.children().stream().mapToInt(TaskQueryService::countConditions).sum();
            case QueryNode.Or or -> or.children().stream().mapToInt(TaskQueryService::countConditions).sum();
            case QueryNode.Not not -> countConditions(not.child());
            case QueryNode.Condition _ -> 1;
        };
    }
}
