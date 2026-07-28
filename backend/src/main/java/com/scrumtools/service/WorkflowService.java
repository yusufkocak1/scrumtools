package com.scrumtools.service;

import com.scrumtools.dto.WorkflowRequest;
import com.scrumtools.dto.WorkflowResponse;
import com.scrumtools.dto.WorkflowStatusRequest;
import com.scrumtools.dto.WorkflowTransitionRequest;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.StatusCategory;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

    // ─── Workflow CRUD ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByTeam(UUID teamId) {
        return workflowRepository.findByTeamId(teamId).stream()
                .map(WorkflowResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByProject(UUID projectId) {
        return workflowRepository.findByProjectId(projectId).stream()
                .map(WorkflowResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(UUID workflowId) {
        Workflow workflow = findById(workflowId);
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public WorkflowResponse createWorkflowForTeam(UUID teamId, WorkflowRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        Workflow workflow = buildWorkflow(request);
        workflow.setTeam(team);
        workflow = workflowRepository.save(workflow);

        log.info("Workflow oluşturuldu: {} (team={})", workflow.getName(), teamId);
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public WorkflowResponse createWorkflowForProject(UUID projectId, WorkflowRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));

        Workflow workflow = buildWorkflow(request);
        workflow.setProject(project);
        workflow = workflowRepository.save(workflow);

        log.info("Workflow oluşturuldu: {} (project={})", workflow.getName(), projectId);
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public WorkflowResponse updateWorkflow(UUID workflowId, WorkflowRequest request) {
        Workflow workflow = findById(workflowId);
        workflow.setName(request.name());
        if (request.description() != null) workflow.setDescription(request.description());
        if (request.isDefault() != null) workflow.setIsDefault(request.isDefault());
        if (request.issueTypes() != null) workflow.setIssueTypes(request.issueTypes());
        workflow = workflowRepository.save(workflow);
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        Workflow workflow = findById(workflowId);
        if (workflow.getIsDefault()) {
            throw new IllegalArgumentException("Varsayılan workflow silinemez.");
        }
        workflowRepository.delete(workflow);
        log.info("Workflow silindi: {}", workflowId);
    }

    // ─── Status CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public WorkflowResponse addStatus(UUID workflowId, WorkflowStatusRequest request) {
        Workflow workflow = findById(workflowId);

        WorkflowStatus status = WorkflowStatus.builder()
                .workflow(workflow)
                .name(request.name())
                .category(request.category() != null ? request.category() : StatusCategory.TO_DO)
                .color(request.color() != null ? request.color() : "#6B7280")
                .icon(request.icon())
                .position(request.position() != null ? request.position() : workflow.getStatuses().size())
                .isInitial(request.isInitial() != null ? request.isInitial() : false)
                .isFinal(request.isFinal() != null ? request.isFinal() : false)
                .isCancellation(request.isCancellation() != null ? request.isCancellation() : false)
                .description(request.description())
                .build();

        workflow.getStatuses().add(status);
        workflow = workflowRepository.save(workflow);
        log.info("Workflow status eklendi: {} → {}", workflow.getName(), request.name());
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public WorkflowResponse updateStatus(UUID workflowId, UUID statusId, WorkflowStatusRequest request) {
        Workflow workflow = findById(workflowId);
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status bulunamadı: " + statusId));

        // Ad değişikliği görevlere de yansıtılır. Görev durumu serbest metin
        // tutulduğu için bu yapılmazsa yeniden adlandırma tüm görevleri
        // katalog dışına düşürür ve board'da yanlış kolona toplanırlar.
        String previousName = status.getName();
        if (request.name() != null && !request.name().isBlank()
                && !request.name().equals(previousName)) {
            status.setName(request.name());
            int moved = reassignTasks(workflow, previousName, request.name());
            if (moved > 0) {
                log.info("Durum yeniden adlandırıldı: '{}' → '{}' ({} görev güncellendi)",
                        previousName, request.name(), moved);
            }
        }

        if (request.category() != null) status.setCategory(request.category());
        if (request.color() != null) status.setColor(request.color());
        if (request.icon() != null) status.setIcon(request.icon());
        if (request.position() != null) status.setPosition(request.position());
        if (request.isFinal() != null) status.setIsFinal(request.isFinal());
        if (request.isCancellation() != null) status.setIsCancellation(request.isCancellation());
        if (request.description() != null) status.setDescription(request.description());

        // Başlangıç durumu tek olmalı — yeni işaretlenen diğerlerini düşürür.
        if (Boolean.TRUE.equals(request.isInitial())) {
            for (WorkflowStatus other : workflow.getStatuses()) {
                if (!other.getId().equals(statusId) && Boolean.TRUE.equals(other.getIsInitial())) {
                    other.setIsInitial(false);
                    workflowStatusRepository.save(other);
                }
            }
            status.setIsInitial(true);
        } else if (Boolean.FALSE.equals(request.isInitial())) {
            status.setIsInitial(false);
        }

        workflowStatusRepository.save(status);
        return WorkflowResponse.from(findById(workflowId));
    }

    /**
     * Durumu siler. Bu durumda görev varsa {@code migrateToStatusId} zorunludur —
     * aksi halde görevler hiçbir kolona düşmeyen bir duruma takılı kalırdı.
     */
    @Transactional
    public void deleteStatus(UUID workflowId, UUID statusId, UUID migrateToStatusId) {
        Workflow workflow = findById(workflowId);
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status bulunamadı: " + statusId));
        if (Boolean.TRUE.equals(status.getIsInitial())) {
            throw new IllegalArgumentException("Başlangıç durumu silinemez. Önce başka bir durumu başlangıç yapın.");
        }
        if (workflow.getStatuses().size() <= 1) {
            throw new IllegalArgumentException("Son durum silinemez; iş akışında en az bir durum bulunmalı.");
        }

        long affected = countTasks(workflow, status.getName());
        if (affected > 0) {
            if (migrateToStatusId == null) {
                throw new IllegalArgumentException(
                        "Bu durumda " + affected + " görev var. Silmeden önce görevlerin taşınacağı durumu seçin.");
            }
            WorkflowStatus target = workflowStatusRepository.findById(migrateToStatusId)
                    .orElseThrow(() -> new IllegalArgumentException("Hedef durum bulunamadı: " + migrateToStatusId));
            if (target.getId().equals(statusId)) {
                throw new IllegalArgumentException("Görevler silinen durumun kendisine taşınamaz.");
            }
            reassignTasks(workflow, status.getName(), target.getName());
            log.info("Durum silindi: '{}' → {} görev '{}' durumuna taşındı",
                    status.getName(), affected, target.getName());
        }

        workflowStatusRepository.delete(status);
    }

    /**
     * Workflow'un kapsamındaki görevlerin durumunu değiştirir.
     * Proje workflow'u yalnız o projenin görevlerini, takım workflow'u takımın
     * tüm görevlerini etkiler.
     */
    private int reassignTasks(Workflow workflow, String oldStatus, String newStatus) {
        if (workflow.getProject() != null) {
            return taskRepository.reassignStatusInProject(workflow.getProject().getId(), oldStatus, newStatus);
        }
        if (workflow.getTeam() != null) {
            return taskRepository.reassignStatusInTeam(workflow.getTeam().getId(), oldStatus, newStatus);
        }
        return 0; // sistem şablonu — bağlı görev yok
    }

    private long countTasks(Workflow workflow, String statusName) {
        if (workflow.getProject() != null) {
            return taskRepository.countByProjectIdAndStatus(workflow.getProject().getId(), statusName);
        }
        if (workflow.getTeam() != null) {
            return taskRepository.countByTeamIdAndStatus(workflow.getTeam().getId(), statusName);
        }
        return 0;
    }

    // ─── Transition CRUD ──────────────────────────────────────────────────────

    @Transactional
    public WorkflowResponse addTransition(UUID workflowId, WorkflowTransitionRequest request) {
        Workflow workflow = findById(workflowId);

        WorkflowStatus fromStatus = request.fromStatusId() != null
                ? workflowStatusRepository.findById(request.fromStatusId()).orElse(null)
                : null;
        WorkflowStatus toStatus = workflowStatusRepository.findById(request.toStatusId())
                .orElseThrow(() -> new IllegalArgumentException("Hedef status bulunamadı: " + request.toStatusId()));

        WorkflowTransition transition = WorkflowTransition.builder()
                .workflow(workflow)
                .name(request.name())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .allowedRoles(request.allowedRoles() != null ? request.allowedRoles() : new ArrayList<>())
                .conditions(request.conditions())
                .actions(request.actions())
                .position(request.position() != null ? request.position() : workflow.getTransitions().size())
                .build();

        workflow.getTransitions().add(transition);
        workflow = workflowRepository.save(workflow);
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public void deleteTransition(UUID workflowId, UUID transitionId) {
        findById(workflowId);
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Geçiş bulunamadı: " + transitionId));
        workflowTransitionRepository.delete(transition);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Workflow findById(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow bulunamadı: " + id));
    }

    private Workflow buildWorkflow(WorkflowRequest request) {
        return Workflow.builder()
                .name(request.name())
                .description(request.description())
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .issueTypes(request.issueTypes() != null ? request.issueTypes() : new ArrayList<>())
                .statuses(new ArrayList<>())
                .transitions(new ArrayList<>())
                .build();
    }
}

