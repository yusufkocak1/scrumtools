package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    // ─── Team bazlı Workflow ───────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/workflows")
    public ResponseEntity<List<WorkflowResponse>> getByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(workflowService.getWorkflowsByTeam(teamId));
    }

    @PostMapping("/api/teams/{teamId}/workflows")
    public ResponseEntity<WorkflowResponse> createForTeam(
            @PathVariable UUID teamId,
            @RequestBody WorkflowRequest request
    ) {
        return ResponseEntity.ok(workflowService.createWorkflowForTeam(teamId, request));
    }

    // ─── Proje bazlı Workflow ─────────────────────────────────────────────────

    @GetMapping("/api/projects/{projectId}/workflows")
    public ResponseEntity<List<WorkflowResponse>> getByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(workflowService.getWorkflowsByProject(projectId));
    }

    @PostMapping("/api/projects/{projectId}/workflows")
    public ResponseEntity<WorkflowResponse> createForProject(
            @PathVariable UUID projectId,
            @RequestBody WorkflowRequest request
    ) {
        return ResponseEntity.ok(workflowService.createWorkflowForProject(projectId, request));
    }

    // ─── Workflow CRUD ────────────────────────────────────────────────────────

    @GetMapping("/api/workflows/{workflowId}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.getWorkflow(workflowId));
    }

    @PutMapping("/api/workflows/{workflowId}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable UUID workflowId,
            @RequestBody WorkflowRequest request
    ) {
        return ResponseEntity.ok(workflowService.updateWorkflow(workflowId, request));
    }

    @DeleteMapping("/api/workflows/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID workflowId) {
        workflowService.deleteWorkflow(workflowId);
        return ResponseEntity.noContent().build();
    }

    // ─── Status yönetimi ──────────────────────────────────────────────────────

    @PostMapping("/api/workflows/{workflowId}/statuses")
    public ResponseEntity<WorkflowResponse> addStatus(
            @PathVariable UUID workflowId,
            @RequestBody WorkflowStatusRequest request
    ) {
        return ResponseEntity.ok(workflowService.addStatus(workflowId, request));
    }

    @PutMapping("/api/workflows/{workflowId}/statuses/{statusId}")
    public ResponseEntity<WorkflowResponse> updateStatus(
            @PathVariable UUID workflowId,
            @PathVariable UUID statusId,
            @RequestBody WorkflowStatusRequest request
    ) {
        return ResponseEntity.ok(workflowService.updateStatus(workflowId, statusId, request));
    }

    @DeleteMapping("/api/workflows/{workflowId}/statuses/{statusId}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable UUID workflowId,
            @PathVariable UUID statusId
    ) {
        workflowService.deleteStatus(workflowId, statusId);
        return ResponseEntity.noContent().build();
    }

    // ─── Transition yönetimi ──────────────────────────────────────────────────

    @PostMapping("/api/workflows/{workflowId}/transitions")
    public ResponseEntity<WorkflowResponse> addTransition(
            @PathVariable UUID workflowId,
            @RequestBody WorkflowTransitionRequest request
    ) {
        return ResponseEntity.ok(workflowService.addTransition(workflowId, request));
    }

    @DeleteMapping("/api/workflows/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<Void> deleteTransition(
            @PathVariable UUID workflowId,
            @PathVariable UUID transitionId
    ) {
        workflowService.deleteTransition(workflowId, transitionId);
        return ResponseEntity.noContent().build();
    }
}

