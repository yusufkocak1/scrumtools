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

    /** Bir dinamik filtrenin açılır listesinde gösterilecek azami seçenek. */
    private static final int DEFAULT_OPTION_LIMIT = 25;

    /**
     * Tek istekte sınıflandırılabilecek azami görev.
     * Board bir sayfada bundan fazlasını göstermiyor; tavan, listeyi elle şişiren
     * bir isteğin tek sorguda binlerce id taşımasını engeller.
     */
    private static final int MAX_CLASSIFY_TASKS = 500;

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
     * Dinamik filtrelerin güncel seçenekleri — tek gidiş-dönüşte hepsi.
     *
     * Seçenekler sabit bir listeden değil, o anki sonuç kümesinden gelir: takımda
     * hiç görevi olmayan bir kişi "Atanan" listesinde görünmez. Her kontrolün
     * seçenekleri, kendi seçimi dışlanarak hesaplanır (bkz. resolveQuery).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> options(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        UUID teamId = filter.getTeam().getId();
        UUID projectId = scopeOf(filter, request);

        List<Map<String, Object>> out = new ArrayList<>();
        for (RichFilterElement element : elementsOf(filter, RichFilterElementKind.DYNAMIC_FILTER)) {
            String field = fieldOf(element);
            if (field == null) continue;

            ParsedQuery scoped = resolveQuery(filter, request, element.getId());
            int limit = intConfig(element, "maxOptions", DEFAULT_OPTION_LIMIT);

            Map<String, Object> control = new LinkedHashMap<>();
            control.put("elementId", element.getId().toString());
            control.put("name", element.getName());
            control.put("field", field);
            control.put("options", aggregationService.aggregate(teamId, projectId, scoped, field, "count", limit));
            out.add(control);
        }
        return out;
    }

    /**
     * İki eksenli gruplama — ısı haritası widget'ı.
     *
     * Satır ekseni verilmezse zengin filtrenin kendi akıllı filtreleri kullanılır;
     * grafikte olduğu gibi burada da varsayılan soru "bu sınıflandırma nasıl dağılıyor".
     */
    @Transactional(readOnly = true)
    public Map<String, Object> matrix(UUID richFilterId, RichFilterRuntimeRequest request) {
        RichFilter filter = accessible(richFilterId);
        String rows = request.getGroupBy() != null && !request.getGroupBy().isBlank()
                ? request.getGroupBy()
                : TaskFieldRegistry.SMART_FILTER_PREFIX + "[" + filter.getName() + "]";

        if (request.getSplitBy() == null || request.getSplitBy().isBlank()) {
            throw new IllegalArgumentException("Isı haritasında ikinci eksen (splitBy) zorunludur.");
        }

        return aggregationService.matrix(filter.getTeam().getId(), scopeOf(filter, request),
                resolveQuery(filter, request), rows, request.getSplitBy(), request.getMetric());
    }

    /**
     * Verilen görevlerin akıllı filtre etiketleri — board ve liste renklendirmesi (Ö2).
     *
     * Board kendi görevlerini kendi sorgusuyla çekiyor; zengin filtre burada
     * filtrelemez, yalnız <b>sınıflandırır</b>. Aynı {@code CASE WHEN} ifadesi
     * kullanıldığı için karttaki renk, panodaki dilimle aynı kuraldan gelir.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> classifyTasks(UUID richFilterId, List<UUID> taskIds, UUID projectId) {
        RichFilter filter = accessible(richFilterId);
        if (taskIds == null || taskIds.isEmpty()) return Map.of();

        List<UUID> limited = taskIds.size() > MAX_CLASSIFY_TASKS
                ? taskIds.subList(0, MAX_CLASSIFY_TASKS)
                : taskIds;

        UUID scope = projectId != null ? projectId
                : (filter.effectiveProject() != null ? filter.effectiveProject().getId() : null);
        return classify(filter, filter.getTeam().getId(), scope, limited);
    }

    private static int intConfig(RichFilterElement element, String key, int fallback) {
        Object value = element.configOrEmpty().get(key);
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
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
     * temel AND (sabit seçimler) AND (dinamik seçimler) AND (akıllı seçimler) AND (metin)
     *
     * Kontroller arası AND, kontrol içi OR. Seçim yoksa o kontrol hiç kısıt
     * eklemez — "hiçbir şey seçilmedi" ile "hepsi seçildi" aynı sonucu verir.
     */
    private ParsedQuery resolveQuery(RichFilter filter, RichFilterRuntimeRequest request) {
        return resolveQuery(filter, request, null);
    }

    /**
     * @param excludeElementId bu öğenin kendi seçimi sorguya katılmaz.
     *
     * Dinamik filtrenin seçeneklerini hesaplarken kendi seçimi dışlanır: "Ahmet"
     * seçiliyken listede yalnız Ahmet kalsaydı, kullanıcı önce seçimi temizlemeden
     * ikinci bir kişiyi ekleyemezdi. Diğer kontroller yine uygulanır — seçenekler
     * gerçekten var olan sonuçları gösterir.
     */
    private ParsedQuery resolveQuery(RichFilter filter, RichFilterRuntimeRequest request,
                                     UUID excludeElementId) {
        ParsedQuery base = QueryParser.parse(filter.effectiveBaseQuery());
        List<List<ParsedQuery>> groups = new ArrayList<>();

        groups.addAll(staticGroups(filter, request, excludeElementId));
        groups.addAll(dynamicGroups(filter, request, excludeElementId));

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
     * Sabit filtreler: yazarın tanımladığı seçeneklerden biri seçilir, seçeneğin
     * kendi STQL'i sorguya AND'lenir. Tek seçimlidir — "Bu hafta / Bu ay / Bu çeyrek"
     * gibi kontrollerde birden çok seçenek aynı anda anlamlı olmaz.
     */
    private List<List<ParsedQuery>> staticGroups(RichFilter filter, RichFilterRuntimeRequest request,
                                                 UUID excludeElementId) {
        Map<UUID, String> selections = request.getStaticSelections();
        if (selections == null || selections.isEmpty()) return List.of();

        List<List<ParsedQuery>> groups = new ArrayList<>();
        for (RichFilterElement element : elementsOf(filter, RichFilterElementKind.STATIC_FILTER)) {
            if (element.getId().equals(excludeElementId)) continue;

            String optionId = selections.get(element.getId());
            if (optionId == null || optionId.isBlank()) continue;

            optionQuery(element, optionId)
                    .ifPresent(query -> groups.add(List.of(QueryParser.parse(query))));
        }
        return groups;
    }

    /** {@code config.options[]} içinden seçilen seçeneğin sorgusu. */
    @SuppressWarnings("unchecked")
    private Optional<String> optionQuery(RichFilterElement element, String optionId) {
        Object raw = element.configOrEmpty().get("options");
        if (!(raw instanceof List<?> options)) return Optional.empty();

        for (Object option : options) {
            if (!(option instanceof Map<?, ?> map)) continue;
            if (!optionId.equals(String.valueOf(((Map<String, Object>) map).get("id")))) continue;

            Object query = ((Map<String, Object>) map).get("query");
            return query == null || query.toString().isBlank()
                    ? Optional.empty()
                    : Optional.of(query.toString());
        }
        return Optional.empty();
    }

    /** Dinamik filtreler: bir alandan seçilen değerler — kontrol içinde OR. */
    private List<List<ParsedQuery>> dynamicGroups(RichFilter filter, RichFilterRuntimeRequest request,
                                                  UUID excludeElementId) {
        Map<UUID, List<String>> selections = request.getDynamic();
        if (selections == null || selections.isEmpty()) return List.of();

        List<List<ParsedQuery>> groups = new ArrayList<>();
        for (RichFilterElement element : elementsOf(filter, RichFilterElementKind.DYNAMIC_FILTER)) {
            if (element.getId().equals(excludeElementId)) continue;

            List<String> values = selections.get(element.getId());
            String field = fieldOf(element);
            if (values == null || values.isEmpty() || field == null) continue;

            // "(Boş)" kovası ayrı ele alınır: değeri olmayan kayıtlar IN listesiyle
            // bulunamaz, SQL'de NULL hiçbir değere eşit değildir.
            List<String> concrete = values.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .toList();
            boolean includeEmpty = values.stream().anyMatch(v -> v == null || v.isBlank());

            List<ParsedQuery> parts = new ArrayList<>();
            if (!concrete.isEmpty()) parts.add(QueryFragments.fieldIn(field, concrete));
            if (includeEmpty) parts.add(QueryFragments.fieldIsEmpty(field));
            if (!parts.isEmpty()) groups.add(parts);
        }
        return groups;
    }

    private static String fieldOf(RichFilterElement element) {
        Object field = element.configOrEmpty().get("field");
        return field == null || field.toString().isBlank() ? null : field.toString();
    }

    private static List<RichFilterElement> elementsOf(RichFilter filter, RichFilterElementKind kind) {
        return filter.getElements().stream()
                .filter(e -> e.getKind() == kind)
                .sorted(Comparator.comparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .toList();
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
