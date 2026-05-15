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
                .description(request.description())
                .build();

        workflow.getStatuses().add(status);
        workflow = workflowRepository.save(workflow);
        log.info("Workflow status eklendi: {} → {}", workflow.getName(), request.name());
        return WorkflowResponse.from(workflow);
    }

    @Transactional
    public WorkflowResponse updateStatus(UUID workflowId, UUID statusId, WorkflowStatusRequest request) {
        findById(workflowId); // existence check
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status bulunamadı: " + statusId));

        if (request.name() != null) status.setName(request.name());
        if (request.category() != null) status.setCategory(request.category());
        if (request.color() != null) status.setColor(request.color());
        if (request.icon() != null) status.setIcon(request.icon());
        if (request.position() != null) status.setPosition(request.position());
        if (request.isInitial() != null) status.setIsInitial(request.isInitial());
        if (request.isFinal() != null) status.setIsFinal(request.isFinal());
        if (request.description() != null) status.setDescription(request.description());

        workflowStatusRepository.save(status);
        return WorkflowResponse.from(findById(workflowId));
    }

    @Transactional
    public void deleteStatus(UUID workflowId, UUID statusId) {
        findById(workflowId);
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status bulunamadı: " + statusId));
        if (status.getIsInitial()) {
            throw new IllegalArgumentException("Başlangıç status'u silinemez.");
        }
        workflowStatusRepository.delete(status);
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

