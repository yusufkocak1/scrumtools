package com.scrumtools.controller;

import com.scrumtools.dto.ReleaseDeploymentResponse;
import com.scrumtools.dto.ReleaseRequest;
import com.scrumtools.dto.ReleaseResponse;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.dto.UpdateReleaseStatusRequest;
import com.scrumtools.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    // ─── Proje bazlı release yönetimi ─────────────────────────────────────────

    @GetMapping("/projects/{projectId}/releases")
    public ResponseEntity<List<ReleaseResponse>> getReleases(@PathVariable UUID projectId) {
        return ResponseEntity.ok(releaseService.getReleasesByProject(projectId));
    }

    @PostMapping("/projects/{projectId}/releases")
    public ResponseEntity<ReleaseResponse> createRelease(
            @PathVariable UUID projectId,
            @RequestBody ReleaseRequest req
    ) {
        return ResponseEntity.ok(releaseService.createRelease(projectId, req, currentUserEmail()));
    }

    @GetMapping("/projects/{projectId}/releases/{releaseId}")
    public ResponseEntity<ReleaseResponse> getRelease(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId
    ) {
        return ResponseEntity.ok(releaseService.getRelease(projectId, releaseId));
    }

    @PutMapping("/projects/{projectId}/releases/{releaseId}")
    public ResponseEntity<ReleaseResponse> updateRelease(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId,
            @RequestBody ReleaseRequest req
    ) {
        return ResponseEntity.ok(releaseService.updateRelease(projectId, releaseId, req, currentUserEmail()));
    }

    @DeleteMapping("/projects/{projectId}/releases/{releaseId}")
    public ResponseEntity<Void> deleteRelease(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId
    ) {
        releaseService.deleteRelease(projectId, releaseId, currentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/projects/{projectId}/releases/{releaseId}/status")
    public ResponseEntity<ReleaseResponse> updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId,
            @RequestBody UpdateReleaseStatusRequest req
    ) {
        return ResponseEntity.ok(releaseService.updateStatus(projectId, releaseId, req.getStatus(), currentUserEmail()));
    }

    @GetMapping("/projects/{projectId}/releases/{releaseId}/tasks")
    public ResponseEntity<List<TaskResponse>> getReleaseTasks(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId
    ) {
        return ResponseEntity.ok(releaseService.getReleaseTasks(projectId, releaseId));
    }

    @GetMapping("/projects/{projectId}/releases/{releaseId}/deployments")
    public ResponseEntity<List<ReleaseDeploymentResponse>> getDeployments(
            @PathVariable UUID projectId,
            @PathVariable UUID releaseId
    ) {
        return ResponseEntity.ok(releaseService.getDeployments(projectId, releaseId));
    }

    // ─── Takım bazlı kolaylık endpoint'leri ───────────────────────────────────

    /** Takımın bağlı olduğu projenin release'leri (task formu dropdown'ı için). */
    @GetMapping("/teams/{teamId}/releases")
    public ResponseEntity<List<ReleaseResponse>> getTeamReleases(@PathVariable UUID teamId) {
        return ResponseEntity.ok(releaseService.getReleasesByTeam(teamId));
    }

    /** Task'ın dağıtım tarihçesi. */
    @GetMapping("/teams/{teamId}/tasks/{taskId}/deployments")
    public ResponseEntity<List<ReleaseDeploymentResponse>> getTaskDeployments(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(releaseService.getTaskDeployments(taskId));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
