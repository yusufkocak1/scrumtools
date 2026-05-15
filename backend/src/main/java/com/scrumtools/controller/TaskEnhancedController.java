package com.scrumtools.controller;

import com.scrumtools.dto.TaskHistoryResponse;
import com.scrumtools.dto.TaskLinkRequest;
import com.scrumtools.dto.TaskLinkResponse;
import com.scrumtools.service.AuditService;
import com.scrumtools.service.TaskLinkService;
import com.scrumtools.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Faz 3 — Alt görev, ilişki, tarihçe, watcher endpoint'leri
 */
@RestController
@RequiredArgsConstructor
public class TaskEnhancedController {

    private final TaskService taskService;
    private final TaskLinkService taskLinkService;
    private final AuditService auditService;

    // ─── Subtasks ─────────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/subtasks")
    public ResponseEntity<List<?>> getSubtasks(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskService.getSubtasks(teamId, taskId));
    }

    // ─── Task Links ───────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/links")
    public ResponseEntity<List<TaskLinkResponse>> getLinks(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskLinkService.getLinks(taskId));
    }

    @PostMapping("/api/teams/{teamId}/tasks/{taskId}/links")
    public ResponseEntity<TaskLinkResponse> createLink(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody TaskLinkRequest request
    ) {
        return ResponseEntity.ok(taskLinkService.createLink(teamId, taskId, request));
    }

    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}/links/{linkId}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable UUID linkId
    ) {
        taskLinkService.deleteLink(teamId, taskId, linkId);
        return ResponseEntity.noContent().build();
    }

    // ─── Task History ─────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/history")
    public ResponseEntity<List<TaskHistoryResponse>> getHistory(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(auditService.getHistory(taskId));
    }

    // ─── Watchers ─────────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/watchers")
    public ResponseEntity<List<String>> getWatchers(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(teamId, taskId).getWatchers());
    }

    @PostMapping("/api/teams/{teamId}/tasks/{taskId}/watchers")
    public ResponseEntity<?> addWatcher(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody Map<String, String> body
    ) {
        String email = body.get("email");
        return ResponseEntity.ok(taskService.addWatcher(teamId, taskId, email));
    }

    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}/watchers/{email}")
    public ResponseEntity<?> removeWatcher(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable String email
    ) {
        return ResponseEntity.ok(taskService.removeWatcher(teamId, taskId, email));
    }
}

