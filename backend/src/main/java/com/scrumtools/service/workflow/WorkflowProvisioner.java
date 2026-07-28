package com.scrumtools.service.workflow;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.Workflow;
import com.scrumtools.entity.WorkflowStatus;
import com.scrumtools.entity.enums.StatusCategory;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Workflow üretimi.
 *
 * {@link TaskStatusService}'ten ayrı bir bean olmasının nedeni işlem sınırı:
 * durum kataloğu rapor gibi {@code readOnly} akışların içinden de isteniyor ve
 * ilk çağrıda workflow'un yazılması gerekebiliyor. {@code REQUIRES_NEW} yalnızca
 * proxy üzerinden geçen çağrılarda etkili olduğundan bu iş ayrı sınıfa alındı —
 * aynı sınıf içinden çağrılsaydı anotasyon yok sayılır, yazma readOnly işleme
 * takılıp sessizce kaybolurdu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowProvisioner {

    private final WorkflowRepository workflowRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    /**
     * Takıma varsayılan workflow üretir.
     *
     * Seed durumlara ek olarak takımın görevlerinde fiilen kullanılan durumlar da
     * eklenir: geçiş anında hiçbir görev kataloğun dışında kalmasın, board'da
     * yanlış kolona toplanmasın diye.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID provisionForTeam(UUID teamId) {
        // Eşzamanlı iki istek aynı takım için tetiklemiş olabilir.
        Workflow existing = pickDefault(workflowRepository.findByTeamId(teamId));
        if (existing != null) return existing.getId();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        Workflow workflow = newWorkflow("Varsayılan İş Akışı",
                "Takımın görev durumları. Ayarlar → İş Akışı ekranından düzenlenebilir.");
        workflow.setTeam(team);

        int position = seedDefaults(workflow);

        Set<String> taken = new LinkedHashSet<>();
        for (WorkflowStatus s : workflow.getStatuses()) taken.add(s.getName().toLowerCase(Locale.ROOT));

        for (String existingStatus : taskRepository.findDistinctStatuses(teamId)) {
            if (existingStatus == null || existingStatus.isBlank()) continue;
            if (!taken.add(existingStatus.toLowerCase(Locale.ROOT))) continue;
            workflow.getStatuses().add(buildStatus(workflow, existingStatus,
                    TaskStatusCatalog.guessCategory(existingStatus), "#6B7280",
                    false, false, false, position++));
        }

        Workflow saved = workflowRepository.save(workflow);
        log.info("Takım {} için varsayılan workflow üretildi ({} durum).",
                teamId, saved.getStatuses().size());
        return saved.getId();
    }

    /**
     * Projeye, varsayılan setten kopyalanan kendi workflow'unu verir.
     * Proje seviyesinde özelleştirme ancak bundan sonra mümkün olur.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID provisionForProject(UUID projectId) {
        Workflow existing = pickDefault(workflowRepository.findByProjectId(projectId));
        if (existing != null) return existing.getId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));

        Workflow workflow = newWorkflow(project.getName() + " İş Akışı",
                "Proje bazlı durum seti — takım varsayılanını geçersiz kılar.");
        workflow.setProject(project);
        seedDefaults(workflow);

        Workflow saved = workflowRepository.save(workflow);
        log.info("Proje {} için workflow üretildi.", projectId);
        return saved.getId();
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private Workflow newWorkflow(String name, String description) {
        return Workflow.builder()
                .name(name)
                .description(description)
                .isDefault(true)
                .issueTypes(new ArrayList<>())
                .statuses(new ArrayList<>())
                .transitions(new ArrayList<>())
                .build();
    }

    /** Seed durumları ekler, sonraki pozisyon numarasını döner. */
    private int seedDefaults(Workflow workflow) {
        int position = 0;
        for (DefaultStatuses.Seed seed : DefaultStatuses.SEEDS) {
            workflow.getStatuses().add(buildStatus(workflow, seed.name(), seed.category(),
                    seed.color(), seed.isInitial(), seed.isFinal(), seed.isCancellation(), position++));
        }
        return position;
    }

    private WorkflowStatus buildStatus(Workflow workflow, String name, StatusCategory category,
                                       String color, boolean isInitial, boolean isFinal,
                                       boolean isCancellation, int position) {
        return WorkflowStatus.builder()
                .workflow(workflow)
                .name(name)
                .category(category)
                .color(color)
                .isInitial(isInitial)
                .isFinal(isFinal)
                .isCancellation(isCancellation)
                .position(position)
                .build();
    }

    private Workflow pickDefault(List<Workflow> workflows) {
        if (workflows == null || workflows.isEmpty()) return null;
        return workflows.stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsDefault()))
                .findFirst()
                .orElse(workflows.get(0));
    }
}
