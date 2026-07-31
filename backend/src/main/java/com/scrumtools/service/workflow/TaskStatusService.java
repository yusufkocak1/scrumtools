package com.scrumtools.service.workflow;

import com.scrumtools.dto.TaskStatusCatalogResponse;
import com.scrumtools.dto.WorkflowResponse;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.Workflow;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Görev durumlarının tek kaynağı.
 *
 * Durumlar artık kodda sabit değil, {@link Workflow} kayıtlarından okunur.
 * Çözümleme sırası:
 * <ol>
 *   <li>Aktif projenin workflow'u (proje bazlı özelleştirme)</li>
 *   <li>Takımın workflow'u</li>
 *   <li>Hiçbiri yoksa takıma varsayılan workflow üretilir</li>
 * </ol>
 *
 * Proje seviyesi override olarak çalışır: proje kendi workflow'unu tanımlamadıysa
 * takımınki geçerlidir, böylece projesiz takımların görevleri de durumsuz kalmaz.
 */
@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final WorkflowProvisioner provisioner;

    // ─── Katalog ──────────────────────────────────────────────────────────────

    /**
     * Takım (ve varsa proje) kapsamındaki geçerli durumlar.
     *
     * Çok sayıda görev üzerinde dönen çağrılar (rapor, sprint kapatma, board)
     * bunu bir kez alıp bellekte kullanmalı — durum başına yeniden çözmemeli.
     */
    @Transactional
    public TaskStatusCatalog getCatalog(UUID teamId, UUID projectId) {
        return toCatalog(resolveWorkflow(teamId, projectId));
    }

    /** Görevin kendi takım/proje kapsamındaki katalog. */
    @Transactional
    public TaskStatusCatalog getCatalogForTask(Task task) {
        return getCatalog(
                task.getTeam() != null ? task.getTeam().getId() : null,
                task.getProject() != null ? task.getProject().getId() : null
        );
    }

    /** Yeni görevlerin başlangıç durumu. */
    @Transactional
    public String initialStatus(UUID teamId, UUID projectId) {
        return getCatalog(teamId, projectId).initialStatusName();
    }

    /** Tek bir durum için "iş bitti mi" sorusu. Döngü içinde katalog kullanın. */
    @Transactional
    public boolean isDone(UUID teamId, UUID projectId, String status) {
        return getCatalog(teamId, projectId).isDone(status);
    }

    private TaskStatusCatalog toCatalog(Workflow workflow) {
        if (workflow == null) return new TaskStatusCatalog(null, null, List.of());
        List<TaskStatusCatalog.StatusView> views = workflow.getStatuses().stream()
                .sorted(Comparator.comparing(s -> s.getPosition() == null ? 0 : s.getPosition()))
                .map(TaskStatusCatalog.StatusView::from)
                .toList();
        return new TaskStatusCatalog(workflow.getId(), workflow.getName(), views);
    }

    // ─── Workflow çözümleme ───────────────────────────────────────────────────

    /**
     * Kapsam için geçerli workflow. Yoksa takım seviyesinde üretilir — durum
     * listesi hiçbir zaman boş dönmesin diye (board kolonları ona bağlı).
     */
    @Transactional
    public Workflow resolveWorkflow(UUID teamId, UUID projectId) {
        if (projectId != null) {
            Workflow projectWorkflow = pickDefault(workflowRepository.findByProjectId(projectId));
            if (projectWorkflow != null) return projectWorkflow;
        }
        if (teamId == null) return null;

        Workflow teamWorkflow = pickDefault(workflowRepository.findByTeamId(teamId));
        if (teamWorkflow != null) return teamWorkflow;

        // Ayrı işlemde üretilir; geçerli persistence context'e almak için tekrar okunur.
        UUID created = provisioner.provisionForTeam(teamId);
        return workflowRepository.findById(created).orElse(null);
    }

    // ─── DTO uçları ───────────────────────────────────────────────────────────
    // Dönüşüm bilinçli olarak burada, işlem içinde yapılıyor: Workflow.statuses
    // LAZY olduğu için controller'a entity dönmek LazyInitializationException'a
    // yol açardı.

    /** Ayarlar ekranının düzenleyeceği workflow. */
    @Transactional
    public WorkflowResponse getEffectiveWorkflow(UUID teamId, UUID projectId) {
        Workflow workflow = resolveWorkflow(teamId, projectId);
        return workflow == null ? null : WorkflowResponse.from(workflow);
    }

    /** Projeye kendi durum setini verir (varsayılan setten kopyalanır). */
    @Transactional
    public WorkflowResponse provisionForProject(UUID projectId) {
        UUID id = provisioner.provisionForProject(projectId);
        return workflowRepository.findById(id).map(WorkflowResponse::from).orElse(null);
    }

    /** Durum listesi + eşlenmemiş uyarıları — arayüzün status registry yanıtı. */
    @Transactional
    public TaskStatusCatalogResponse getCatalogResponse(UUID teamId, UUID projectId) {
        Workflow workflow = resolveWorkflow(teamId, projectId);
        TaskStatusCatalog catalog = toCatalog(workflow);

        // Kapsam bilgisi: proje kendi setini tanımladı mı, yoksa takımınkini mi
        // kullanıyor. Ayarlar ekranı "bu proje takım ayarını kullanıyor" uyarısını
        // buna göre gösterir.
        boolean projectScoped = projectId != null
                && workflow != null
                && workflow.getProject() != null
                && projectId.equals(workflow.getProject().getId());

        return TaskStatusCatalogResponse.of(catalog,
                projectScoped ? "PROJECT" : "TEAM",
                unmappedFor(teamId, catalog));
    }

    /**
     * Durum adı → o durumdaki görev sayısı (anahtarlar küçük harf).
     *
     * Sütun eşleme ekranı her durumun kaç iş taşıdığını gösterir: boş bir durumu
     * board dışında bırakmakla 36 işi taşıyan durumu dışarıda bırakmak aynı şey
     * değil. Durum serbest metin olduğu için "In Progress" ve "in progress" tek
     * kovada toplanır.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusCounts(UUID teamId, UUID projectId) {
        List<Object[]> rows = projectId != null
                ? taskRepository.countByStatusInProject(projectId)
                : taskRepository.countByStatus(teamId);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            counts.merge(row[0].toString().toLowerCase(Locale.ROOT),
                    ((Number) row[1]).longValue(), Long::sum);
        }
        return counts;
    }

    /** isDefault işaretli olan tercih edilir; yoksa ilk kayıt. */
    private Workflow pickDefault(List<Workflow> workflows) {
        if (workflows == null || workflows.isEmpty()) return null;
        return workflows.stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsDefault()))
                .findFirst()
                .orElse(workflows.get(0));
    }

    // ─── Tutarlılık ───────────────────────────────────────────────────────────

    /**
     * Takımın görevlerinde geçip katalogda karşılığı olmayan durumlar.
     * Ayarlar ekranı bunları "eşlenmemiş" uyarısı olarak gösterir — durum serbest
     * metin kabul edildiği için bu bir hata değil, farkındalık bilgisidir.
     */
    @Transactional
    public List<String> findUnmappedStatuses(UUID teamId, UUID projectId) {
        return unmappedFor(teamId, getCatalog(teamId, projectId));
    }

    private List<String> unmappedFor(UUID teamId, TaskStatusCatalog catalog) {
        return taskRepository.findDistinctStatuses(teamId).stream()
                .filter(s -> s != null && !s.isBlank())
                .filter(s -> !catalog.contains(s))
                .toList();
    }
}
