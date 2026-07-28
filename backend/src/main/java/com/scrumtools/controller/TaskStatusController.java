package com.scrumtools.controller;

import com.scrumtools.dto.TaskStatusCatalogResponse;
import com.scrumtools.dto.WorkflowResponse;
import com.scrumtools.service.workflow.TaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Görev durumlarının okuma ucu.
 *
 * Durum tanımlarının düzenlenmesi {@link WorkflowController} üzerinden yürür
 * (workflow/status CRUD); burası "şu an hangi durumlar geçerli" sorusunu
 * cevaplar ve gerekiyorsa workflow'u üretir.
 */
@RestController
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    /**
     * Takım (ve varsa aktif proje) kapsamında geçerli durum listesi.
     * Workflow yoksa ilk çağrıda takıma varsayılan workflow üretilir.
     */
    @GetMapping("/api/teams/{teamId}/statuses")
    public ResponseEntity<TaskStatusCatalogResponse> getStatuses(
            @PathVariable UUID teamId,
            @RequestParam(required = false) UUID projectId
    ) {
        return ResponseEntity.ok(taskStatusService.getCatalogResponse(teamId, projectId));
    }

    /** Ayarlar ekranının düzenleyeceği workflow — durum ve geçişleriyle birlikte. */
    @GetMapping("/api/teams/{teamId}/workflows/effective")
    public ResponseEntity<WorkflowResponse> getEffectiveWorkflow(
            @PathVariable UUID teamId,
            @RequestParam(required = false) UUID projectId
    ) {
        return ResponseEntity.ok(taskStatusService.getEffectiveWorkflow(teamId, projectId));
    }

    /**
     * Projeye kendi durum setini verir (varsayılan setten kopyalanır).
     * Bundan sonra proje, takımdan bağımsız düzenlenebilir.
     */
    @PostMapping("/api/projects/{projectId}/workflows/provision")
    public ResponseEntity<WorkflowResponse> provisionProjectWorkflow(@PathVariable UUID projectId) {
        return ResponseEntity.ok(taskStatusService.provisionForProject(projectId));
    }
}
