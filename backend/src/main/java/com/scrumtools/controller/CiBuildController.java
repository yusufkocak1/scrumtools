package com.scrumtools.controller;

import com.scrumtools.dto.CiBuildResponse;
import com.scrumtools.dto.CiReleasePipelineView;
import com.scrumtools.dto.CiTaskDeployView;
import com.scrumtools.dto.CiTriggerRequest;
import com.scrumtools.service.ci.CiBuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Build tetikleme, tarihçe ve durum yenileme uçları.
 * Yetki/entitlement/rate-limit kontrolleri servis katmanındadır:
 * TASK_DEPLOY proje üyesine, RELEASE_PIPELINE release manager/org admin'e açıktır.
 */
@RestController
@RequiredArgsConstructor
public class CiBuildController {

    private final CiBuildService buildService;

    // ─── Task deploy ──────────────────────────────────────────────────────────

    @GetMapping("/api/tasks/{taskId}/ci")
    public ResponseEntity<CiTaskDeployView> taskDeployView(@PathVariable UUID taskId) {
        return ResponseEntity.ok(buildService.getTaskDeployView(taskId, currentUserEmail()));
    }

    @GetMapping("/api/tasks/{taskId}/ci/builds")
    public ResponseEntity<List<CiBuildResponse>> taskBuilds(@PathVariable UUID taskId) {
        return ResponseEntity.ok(buildService.listTaskBuilds(taskId, currentUserEmail()));
    }

    @PostMapping("/api/tasks/{taskId}/ci/deploy")
    public ResponseEntity<CiBuildResponse> deploy(
            @PathVariable UUID taskId,
            @RequestBody CiTriggerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildService.triggerForTask(taskId, currentUserEmail(), request));
    }

    // ─── Release pipeline ─────────────────────────────────────────────────────

    @GetMapping("/api/releases/{releaseId}/ci")
    public ResponseEntity<CiReleasePipelineView> releasePipelineView(@PathVariable UUID releaseId) {
        return ResponseEntity.ok(buildService.getReleasePipelineView(releaseId, currentUserEmail()));
    }

    @GetMapping("/api/releases/{releaseId}/ci/builds")
    public ResponseEntity<List<CiBuildResponse>> releaseBuilds(@PathVariable UUID releaseId) {
        return ResponseEntity.ok(buildService.listReleaseBuilds(releaseId, currentUserEmail()));
    }

    @PostMapping("/api/releases/{releaseId}/ci/run")
    public ResponseEntity<CiBuildResponse> run(
            @PathVariable UUID releaseId,
            @RequestBody CiTriggerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildService.triggerForRelease(releaseId, currentUserEmail(), request));
    }

    // ─── Proje geneli tarihçe + manuel yenileme ───────────────────────────────

    @GetMapping("/api/projects/{projectId}/ci/builds")
    public ResponseEntity<Page<CiBuildResponse>> projectBuilds(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(buildService.listProjectBuilds(projectId, currentUserEmail(), status, page));
    }

    @PostMapping("/api/ci/builds/{buildId}/refresh")
    public ResponseEntity<CiBuildResponse> refresh(@PathVariable UUID buildId) {
        return ResponseEntity.ok(buildService.refresh(buildId, currentUserEmail()));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
