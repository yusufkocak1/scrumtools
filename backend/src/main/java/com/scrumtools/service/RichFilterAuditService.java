package com.scrumtools.service;

import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.User;
import com.scrumtools.query.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Sınıflandırma denetimi — "bu kural sırası gerçekte ne yapıyor?" (Ö5).
 *
 * Akıllı filtrelerde <b>ilk eşleşen kazanır</b> (K4). Yedi kuralı doğru sıralamak
 * gözle zordur: üçüncü kural kendi başına 60 göreve uyuyor olabilir ama ikisi ondan
 * önce geldiği için sadece 12'sini alıyordur. Ekranda görünen tek şey "12"dir; geri
 * kalan 48'in nereye gittiği görünmez. Bu servis o farkı sayıya döker.
 *
 * Hepsi <b>tek sorguda</b> çıkar: sınıflandırma ifadesine göre gruplanır ve her
 * kural için ayrı bir {@code SUM(CASE WHEN kural THEN 1 ELSE 0 END)} sütunu seçilir.
 * Böylece satır = görevi sahiplenen kural, sütun = göreve uyan kural olur; kesişim
 * doğrudan "kim kimden aldı" matrisidir. Kural başına ayrı sayım sorgusu atmak
 * yedi kuralda 1 + 7 + 49 gidiş-dönüş demek olurdu.
 */
@Service
@RequiredArgsConstructor
public class RichFilterAuditService {

    /** Sınıflandırılmamış görevlerden gösterilecek örnek sayısı. */
    private static final int SAMPLE_SIZE = 8;

    /** Sınıflandırılmamış kovanın anahtarı — {@link QueryPredicateBuilder} ile aynı. */
    private static final String UNCLASSIFIED = "";

    private final RichFilterService richFilterService;
    private final TaskQueryService taskQueryService;
    private final EntityManager em;

    /**
     * @return {@code {total, unclassified:{count,stql,samples}, clauses:[…]}}
     *         — her kural için {@code owned} (sahiplendiği), {@code matched}
     *         (kendi başına uyduğu) ve {@code takenBy} (kaybettiklerini kimin aldığı)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> audit(UUID richFilterId, UUID projectId) {
        User user = richFilterService.currentUserOrThrow();
        RichFilter filter = richFilterService.requireAccessible(richFilterId, user);

        UUID teamId = filter.getTeam().getId();
        UUID scope = projectId != null ? projectId
                : (filter.effectiveProject() != null ? filter.effectiveProject().getId() : null);

        QueryContext ctx = taskQueryService.buildContext(teamId, scope);
        List<SmartClause> clauses = ctx.smartClauses(filter.getName());
        if (clauses == null || clauses.isEmpty()) {
            return empty();
        }

        ParsedQuery base = QueryParser.parse(filter.effectiveBaseQuery());
        Grid grid = runGrid(ctx, base, clauses);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", grid.total());
        out.put("clauses", clauseReports(clauses, grid));
        out.put("unclassified", unclassified(filter, teamId, scope, base, grid));
        return out;
    }

    // ─── Matris sorgusu ───────────────────────────────────────────────────────

    /**
     * @param owned     kural id → o kuralın sahiplendiği görev sayısı
     * @param overlap   sahiplenen kural id → (uyan kural id → sayı)
     * @param total     temel sorgudaki toplam görev
     */
    private record Grid(Map<String, Long> owned, Map<String, Map<String, Long>> overlap, long total) {
    }

    private Grid runGrid(QueryContext ctx, ParsedQuery base, List<SmartClause> clauses) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Task> root = query.from(Task.class);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, query, ctx);
        List<Predicate> predicates = new ArrayList<>(builder.scopePredicates());
        if (base.hasWhere()) predicates.add(builder.build(base.where()));
        query.where(cb.and(predicates.toArray(new Predicate[0])));

        Expression<String> bucket = builder.smartBucketExpression(clauses, UNCLASSIFIED);

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(bucket);
        for (SmartClause clause : clauses) {
            // Kuralın kendi başına eşleşmesi — sıra gözetmeden (bkz. clauseMatchPredicate).
            selections.add(cb.sum(cb.<Integer>selectCase()
                    .when(builder.clauseMatchPredicate(clause), 1)
                    .otherwise(0)));
        }
        selections.add(cb.count(root.get("id")));

        query.multiselect(selections);
        query.groupBy(bucket);

        Map<String, Long> owned = new LinkedHashMap<>();
        Map<String, Map<String, Long>> overlap = new LinkedHashMap<>();
        long total = 0;

        int countIndex = clauses.size() + 1;
        for (Tuple row : em.createQuery(query).getResultList()) {
            String ownerKey = row.get(0) == null ? UNCLASSIFIED : row.get(0).toString();
            long rowCount = number(row.get(countIndex));

            owned.put(ownerKey, rowCount);
            total += rowCount;

            Map<String, Long> matches = new LinkedHashMap<>();
            for (int i = 0; i < clauses.size(); i++) {
                long matched = number(row.get(i + 1));
                if (matched > 0) matches.put(clauses.get(i).id().toString(), matched);
            }
            overlap.put(ownerKey, matches);
        }
        return new Grid(owned, overlap, total);
    }

    // ─── Raporlama ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> clauseReports(List<SmartClause> clauses, Grid grid) {
        List<Map<String, Object>> out = new ArrayList<>();

        for (SmartClause clause : clauses) {
            String id = clause.id().toString();
            long ownedCount = grid.owned().getOrDefault(id, 0L);

            // Kendi başına kaç göreve uyuyor: bütün satırlardaki eşleşmelerin toplamı.
            long matched = grid.overlap().values().stream()
                    .mapToLong(row -> row.getOrDefault(id, 0L))
                    .sum();

            // Kaybettiklerini kim aldı: bu kurala uyan ama başkasının sahiplendiği görevler.
            List<Map<String, Object>> takenBy = new ArrayList<>();
            for (SmartClause other : clauses) {
                if (other.id().equals(clause.id())) continue;
                long stolen = grid.overlap().getOrDefault(other.id().toString(), Map.of())
                        .getOrDefault(id, 0L);
                if (stolen > 0) {
                    takenBy.add(Map.of(
                            "id", other.id().toString(),
                            "name", other.name() == null ? "" : other.name(),
                            "count", stolen));
                }
            }

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("id", id);
            report.put("name", clause.name());
            report.put("color", clause.color());
            report.put("owned", ownedCount);
            report.put("matched", matched);
            report.put("shadowed", Math.max(0, matched - ownedCount));
            report.put("takenBy", takenBy);
            // Hiç görev sahiplenmeyen kural: ya sorgusu hiçbir şeye uymuyor ya da
            // tamamen önceki kuralların gölgesinde. İkisi de düzeltilmesi gereken durum.
            report.put("dead", ownedCount == 0);
            out.add(report);
        }
        return out;
    }

    /**
     * Hiçbir kurala uymayan görevler: sayı, örnekler ve listede açmak için STQL.
     *
     * Örnekler önemli: "12 görev sınıflandırılmamış" cümlesi tek başına eyleme
     * dönüşmez, hangi görevler olduğunu görmek eksik kuralı çoğu zaman anında
     * anlatır.
     */
    private Map<String, Object> unclassified(RichFilter filter, UUID teamId, UUID projectId,
                                             ParsedQuery base, Grid grid) {
        long count = grid.owned().getOrDefault(UNCLASSIFIED, 0L);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", count);
        if (count == 0) {
            out.put("stql", null);
            out.put("samples", List.of());
            return out;
        }

        ParsedQuery query = QueryComposer.compose(base,
                List.of(List.of(QueryFragments.smartIsEmpty(filter.getName()))));

        out.put("stql", StqlRenderer.render(query));
        out.put("samples", taskQueryService
                .execute(teamId, projectId, query, null, null, 0, SAMPLE_SIZE)
                .get("content"));
        return out;
    }

    private static Map<String, Object> empty() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", 0L);
        out.put("clauses", List.of());
        out.put("unclassified", Map.of("count", 0L, "stql", null, "samples", List.of()));
        return out;
    }

    private static long number(Object raw) {
        return raw instanceof Number n ? n.longValue() : 0L;
    }
}
