package com.scrumtools.service;

import com.scrumtools.dto.RichFilterRuntimeRequest;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.RichFilterElementKind;
import com.scrumtools.query.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Zengin filtrenin çalıştırılması: seçimler → sorgu → sonuç.
 *
 * Sorgu <b>ağaç düzeyinde</b> kurulur (bkz. {@link QueryComposer}); seçimler
 * metne çevrilip yeniden çözümlenmez. Çalıştırma uçları paket denetimine tabi
 * değildir — paylaşılan bir zengin filtreyi görüntülemek her pakette serbesttir
 * (bkz. RICH_FILTER_PLAN.md — K16).
 */
@Service
@RequiredArgsConstructor
public class RichFilterRuntimeService {

    /** Sınıflandırılmamış görevlerin kova anahtarı — arayüzde "Sınıflandırılmamış". */
    private static final String UNCLASSIFIED = "";

    private final RichFilterService richFilterService;
    private final TaskQueryService taskQueryService;
    private final TaskAggregationService aggregationService;
    private final EntityManager em;

    // ─── Çalıştırma ───────────────────────────────────────────────────────────

    /** Sayfalı görev listesi + her görevin akıllı filtre etiketi. */
    @Transactional(readOnly = true)
    public Map<String, Object> search(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        ParsedQuery parsed = resolveQuery(filter, request);
        UUID teamId = filter.getTeam().getId();
        UUID projectId = scopeOf(filter, request);

        Map<String, Object> result = new LinkedHashMap<>(
                taskQueryService.execute(teamId, projectId, parsed, null, null,
                        request.getPage(), request.getSize()));

        result.put("smartTags", classify(filter, teamId, projectId, taskIdsOf(result)));
        return result;
    }

    /** Eşleşen kayıt sayısı — sayaç widget'ı için tek çağrı. */
    @Transactional(readOnly = true)
    public long count(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        return taskQueryService.count(filter.getTeam().getId(), scopeOf(filter, request),
                resolveQuery(filter, request));
    }

    /**
     * Gruplama. {@code groupBy} verilmezse zengin filtrenin kendi akıllı filtreleri
     * eksen olur — bir zengin filtreye bağlı grafiğin varsayılan sorusu budur.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> aggregate(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        String groupBy = request.getGroupBy() != null && !request.getGroupBy().isBlank()
                ? request.getGroupBy()
                : TaskFieldRegistry.SMART_FILTER_PREFIX + "[" + filter.getName() + "]";

        return aggregationService.aggregate(filter.getTeam().getId(), scopeOf(filter, request),
                resolveQuery(filter, request), groupBy, request.getMetric(), request.getLimit());
    }

    /**
     * Seçimlerin karşılığı olan STQL metni ve eşleşen kayıt sayısı.
     *
     * Grafikten görev listesine geçişin köprüsü: arayüz bu metni mevcut
     * {@code /workList?q=} ekranına taşır, ayrı bir drill-down ekranı gerekmez.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolve(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        ParsedQuery parsed = resolveQuery(filter, request);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stql", StqlRenderer.render(parsed));
        out.put("count", taskQueryService.count(filter.getTeam().getId(), scopeOf(filter, request), parsed));
        return out;
    }

    // ─── Sorgu kurulumu ───────────────────────────────────────────────────────

    /**
     * temel AND (akıllı seçimler) AND (metin arama)
     *
     * Kontroller arası AND, kontrol içi OR. Seçim yoksa o kontrol hiç kısıt
     * eklemez — "hiçbir şey seçilmedi" ile "hepsi seçildi" aynı sonucu verir.
     */
    private ParsedQuery resolveQuery(RichFilter filter, RichFilterRuntimeRequest request) {
        ParsedQuery base = QueryParser.parse(filter.effectiveBaseQuery());
        List<List<ParsedQuery>> groups = new ArrayList<>();

        List<String> smartNames = selectedSmartNames(filter, request.getSmart());
        if (!smartNames.isEmpty()) {
            groups.add(List.of(QueryFragments.smartIn(filter.getName(), smartNames)));
        }

        String text = request.getText();
        if (text != null && !text.isBlank()) {
            groups.add(List.of(QueryFragments.textSearch(text.trim())));
        }

        return QueryComposer.compose(base, groups);
    }

    /**
     * Seçili id'leri akıllı filtre adlarına çevirir.
     *
     * Silinmiş bir öğenin id'si sessizce atlanır: dashboard'daki eski bir seçim
     * yüzünden widget'ın hata vermesi, kullanıcının anlamlandıramayacağı bir
     * kırılma olurdu.
     */
    private List<String> selectedSmartNames(RichFilter filter, List<UUID> selected) {
        if (selected == null || selected.isEmpty()) return List.of();

        Set<UUID> wanted = new HashSet<>(selected);
        return filter.getElements().stream()
                .filter(e -> e.getKind() == RichFilterElementKind.SMART_FILTER)
                .filter(e -> wanted.contains(e.getId()))
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .map(RichFilterElement::getName)
                .toList();
    }

    // ─── Sınıflandırma ────────────────────────────────────────────────────────

    /**
     * Dönen sayfadaki görevlerin akıllı filtre etiketleri.
     *
     * Liste sorgusuna eklenmek yerine ayrı çalışır: {@code TaskResponse} şeması
     * bozulmadan kalır ve etiketler yalnız görüntülenen sayfa için, birincil
     * anahtar üzerinden tek sorguda hesaplanır.
     */
    private Map<String, Object> classify(RichFilter filter, UUID teamId, UUID projectId, List<UUID> taskIds) {
        if (taskIds.isEmpty()) return Map.of();

        QueryContext ctx = taskQueryService.buildContext(teamId, projectId);
        List<SmartClause> clauses = ctx.smartClauses(filter.getName());
        if (clauses == null || clauses.isEmpty()) return Map.of();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Task> root = query.from(Task.class);

        QueryPredicateBuilder builder = new QueryPredicateBuilder(cb, root, query, ctx);
        Expression<String> bucket = builder.smartBucketExpression(clauses, UNCLASSIFIED);

        query.multiselect(root.get("id"), bucket).where(root.get("id").in(taskIds));

        Map<UUID, SmartClause> byId = new HashMap<>();
        clauses.forEach(c -> byId.put(c.id(), c));

        Map<String, Object> tags = new LinkedHashMap<>();
        for (Tuple row : em.createQuery(query).getResultList()) {
            String clauseId = (String) row.get(1);
            if (clauseId == null || clauseId.isBlank()) continue;

            SmartClause clause = byId.get(UUID.fromString(clauseId));
            if (clause == null) continue;

            tags.put(row.get(0).toString(), Map.of(
                    "id", clause.id().toString(),
                    "name", clause.name() == null ? "" : clause.name(),
                    "color", clause.color() == null ? "" : clause.color()));
        }
        return tags;
    }

    /** Sayfadaki görevlerin id'leri. TaskResponse id'yi metin taşır; burada UUID'ye çevrilir. */
    private static List<UUID> taskIdsOf(Map<String, Object> result) {
        if (!(result.get("content") instanceof List<?> list)) return List.of();

        List<UUID> ids = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof TaskResponse task) || task.getId() == null) continue;
            try {
                ids.add(UUID.fromString(task.getId()));
            } catch (IllegalArgumentException ignored) {
                // Beklenmedik biçim: o görev etiketsiz kalır, liste yine döner.
            }
        }
        return ids;
    }

    // ─── Yetki ve kapsam ──────────────────────────────────────────────────────

    private RichFilter accessible(UUID richFilterId) {
        User user = richFilterService.currentUserOrThrow();
        return richFilterService.requireAccessible(richFilterId, user);
    }

    private static UUID scopeOf(RichFilter filter, RichFilterRuntimeRequest request) {
        if (request != null && request.getProjectId() != null) return request.getProjectId();
        return filter.effectiveProject() != null ? filter.effectiveProject().getId() : null;
    }
}
