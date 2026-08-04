package com.scrumtools.query;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.*;
import com.scrumtools.service.workflow.TaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

/**
 * STQL editörünün otomatik tamamlama kaynağı.
 *
 * Alan kataloğunu {@link TaskFieldRegistry}'den, değer önerilerini ise takımın
 * gerçek verisinden üretir — böylece kullanıcı yalnızca var olan değerleri görür.
 */
@Service
@RequiredArgsConstructor
public class QuerySuggestionService {

    /** Tek istekte dönebilecek azami öneri sayısı. */
    private static final int MAX_SUGGESTIONS = 50;

    /** Veri tabanında karşılığı olmayan, uygulama tarafından sabitlenmiş değerler. */
    private static final List<String> DEFAULT_PRIORITIES = List.of("Critical", "High", "Medium", "Low");
    private static final List<String> DEFAULT_ISSUE_TYPES = List.of("task", "story", "bug", "epic");
    private static final List<String> DEFAULT_RESOLUTIONS =
            List.of("Fixed", "Won't Fix", "Duplicate", "Cannot Reproduce");

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SprintRepository sprintRepository;
    private final ReleaseRepository releaseRepository;
    private final TaskStatusService taskStatusService;
    private final SmartFilterCatalog smartFilterCatalog;

    /** Alan kataloğu — arayüz bunu alan ve operatör listesi olarak gösterir. */
    public Map<String, Object> fieldCatalog() {
        List<Map<String, Object>> fields = TaskFieldRegistry.all().stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", f.name());
                    m.put("label", f.label());
                    m.put("type", f.type().name());
                    m.put("aliases", f.aliases().stream().sorted().toList());
                    m.put("operators", f.type().operators().stream().map(QueryOperator::symbol).sorted().toList());
                    m.put("hasSuggestions", f.suggestSource() != null);
                    // Grafik yapılandırması alan listesini buradan süzer: grup ekseni
                    // ve ölçü açılır listeleri ayrı bir katalog tutmaz.
                    m.put("groupable", f.type().groupable());
                    m.put("summable", f.type().summable());
                    return m;
                })
                .toList();

        List<Map<String, String>> functions = QueryFunctions.CATALOG.stream()
                .map(f -> Map.of("name", f[0], "description", f[1]))
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fields", fields);
        out.put("functions", functions);
        out.put("keywords", List.of("AND", "OR", "NOT", "IN", "IS EMPTY", "IS NOT EMPTY", "ORDER BY", "ASC", "DESC"));
        out.put("customFieldSyntax", "cf[alanAnahtari]");
        out.put("smartFilterSyntax", "smart[\"zengin filtre adı\"]");
        return out;
    }

    /**
     * Bir alan için değer önerileri.
     *
     * @param field  STQL alan adı (alias kabul edilir)
     * @param prefix kullanıcının yazmakta olduğu ön ek; boş olabilir
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> suggestValues(UUID teamId, UUID projectId, String field, String prefix) {
        FieldDescriptor descriptor = TaskFieldRegistry.resolve(field).orElse(null);
        if (descriptor == null) return List.of();

        String q = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);

        // smart["zengin filtre"] alanında öneriler o filtrenin akıllı filtre adlarıdır;
        // katalog kaynağı sabit bir liste değil, kullanıcının kurduğu sınıflandırmadır.
        if (descriptor.type() == FieldType.SMART_FILTER) {
            return filterByPrefix(smartFilterSuggestions(teamId, descriptor.path()), q);
        }
        if (descriptor.suggestSource() == null) return List.of();

        List<Map<String, String>> raw = switch (descriptor.suggestSource()) {
            case "users" -> userSuggestions(teamId);
            case "labels" -> plain(taskRepository.findDistinctLabels(teamId));
            // Önce workflow'da tanımlı durumlar (sıralı), sonra veride kalmış
            // ama artık tanımlı olmayanlar — sorgu yazan kişi ikisini de görmeli.
            case "statuses" -> plain(merge(taskRepository.findDistinctStatuses(teamId),
                    taskStatusService.getCatalog(teamId, projectId).names()));
            case "priorities" -> plain(DEFAULT_PRIORITIES);
            case "issueTypes" -> plain(DEFAULT_ISSUE_TYPES);
            case "resolutions" -> plain(merge(taskRepository.findDistinctResolutions(teamId), DEFAULT_RESOLUTIONS));
            case "environments" -> plain(taskRepository.findDistinctEnvironments(teamId));
            case "sprints" -> sprintSuggestions(teamId);
            case "projects" -> projectSuggestions(teamId);
            case "releases" -> releaseSuggestions(teamId, projectId);
            default -> List.of();
        };

        return filterByPrefix(raw, q);
    }

    private static List<Map<String, String>> filterByPrefix(List<Map<String, String>> raw, String q) {
        return raw.stream()
                .filter(m -> q.isEmpty()
                        || m.get("value").toLowerCase(Locale.ROOT).contains(q)
                        || m.getOrDefault("label", "").toLowerCase(Locale.ROOT).contains(q))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    // ─── Kaynaklar ────────────────────────────────────────────────────────────

    /** Zengin filtrenin akıllı filtre adları — sırası sınıflandırma önceliğidir. */
    private List<Map<String, String>> smartFilterSuggestions(UUID teamId, String richFilterName) {
        List<SmartClause> clauses = smartFilterCatalog.clausesOf(teamId, richFilterName);
        if (clauses == null) return List.of();
        return clauses.stream().map(c -> entry(c.name(), c.name())).toList();
    }

    private List<Map<String, String>> userSuggestions(UUID teamId) {
        return teamMemberRepository.findByTeamId(teamId).stream()
                .filter(m -> m.getEmail() != null)
                .map(m -> entry(m.getEmail(),
                        m.getDisplayName() != null && !m.getDisplayName().isBlank()
                                ? m.getDisplayName() : m.getEmail()))
                .toList();
    }

    private List<Map<String, String>> sprintSuggestions(UUID teamId) {
        return sprintRepository.findByTeamId(teamId).stream()
                .map(s -> entry(s.getName(), s.getName() + " (" + s.getStatus() + ")"))
                .toList();
    }

    private List<Map<String, String>> projectSuggestions(UUID teamId) {
        return teamRepository.findById(teamId)
                .map(Team::getProjects)
                .orElse(Set.of())
                .stream()
                .map(p -> entry(p.getKey() != null ? p.getKey() : p.getName(), p.getName()))
                .toList();
    }

    /**
     * Sürümler proje bazlıdır. Aktif proje kapsamı varsa yalnız onunkiler,
     * yoksa takımın çalıştığı tüm projelerinkiler önerilir.
     */
    private List<Map<String, String>> releaseSuggestions(UUID teamId, UUID projectId) {
        Stream<UUID> projectIds = projectId != null
                ? Stream.of(projectId)
                : teamRepository.findById(teamId)
                        .map(Team::getProjects).orElse(Set.of())
                        .stream().map(Project::getId);

        return projectIds
                .flatMap(pid -> releaseRepository.findByProjectIdOrderByCreatedAtDesc(pid).stream())
                .map(r -> entry(r.getName(), r.getName() + " (" + r.getStatus() + ")"))
                .toList();
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private static List<Map<String, String>> plain(Collection<String> values) {
        return values.stream().filter(Objects::nonNull).map(v -> entry(v, v)).toList();
    }

    private static Map<String, String> entry(String value, String label) {
        return Map.of("value", value == null ? "" : value, "label", label == null ? "" : label);
    }

    /** Veriden gelen değerleri varsayılanlarla birleştirir; sıra korunur, tekrar elenir. */
    private static List<String> merge(List<String> fromData, List<String> defaults) {
        LinkedHashSet<String> set = new LinkedHashSet<>(defaults);
        set.addAll(fromData);
        return List.copyOf(set);
    }
}
