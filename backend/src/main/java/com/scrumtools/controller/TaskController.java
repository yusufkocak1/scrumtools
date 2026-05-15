package com.scrumtools.controller;

import com.scrumtools.dto.AddCommentRequest;
import com.scrumtools.dto.TaskFilterRequest;
import com.scrumtools.dto.TaskRequest;
import com.scrumtools.dto.TaskResponse;
import com.scrumtools.service.TaskFilterService;
import com.scrumtools.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskFilterService taskFilterService;

    // ─── Task Filter (Faz 4) ──────────────────────────────────────────────────

    /**
     * Dinamik JSON filtre motoru.
     * POST /api/teams/{teamId}/tasks/filter
     * Body: { filters: [{field, operator, values}], sortBy, sortDir, page, size }
     */
    @PostMapping("/api/teams/{teamId}/tasks/filter")
    public ResponseEntity<Map<String, Object>> filterTasks(
            @PathVariable UUID teamId,
            @RequestBody TaskFilterRequest req
    ) {
        return ResponseEntity.ok(taskFilterService.filter(teamId, req));
    }

    // ─── Task CRUD ────────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "false") boolean includeCancelled
    ) {
        return ResponseEntity.ok(taskService.getTasksByTeam(teamId, includeCancelled));
    }

    @GetMapping("/api/teams/{teamId}/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(teamId, taskId));
    }

    /**
     * Cross-team customId arama — kullanıcının üye olduğu tüm takımlarda arar.
     * Örn: GET /api/tasks/search?customId=TEAM-5
     */
    @GetMapping("/api/tasks/search")
    public ResponseEntity<?> searchByCustomId(@RequestParam String customId) {
        return taskService.findByCustomId(customId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/teams/{teamId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID teamId,
            @RequestBody TaskRequest req
    ) {
        return ResponseEntity.ok(taskService.createTask(teamId, req));
    }

    @PutMapping("/api/teams/{teamId}/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody TaskRequest req
    ) {
        return ResponseEntity.ok(taskService.updateTask(teamId, taskId, req));
    }

    @PatchMapping("/api/teams/{teamId}/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(taskService.updateStatus(teamId, taskId, body.get("status")));
    }

    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        taskService.deleteTask(teamId, taskId);
        return ResponseEntity.noContent().build();
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @PostMapping("/api/teams/{teamId}/tasks/{taskId}/comments")
    public ResponseEntity<TaskResponse> addComment(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody AddCommentRequest req
    ) {
        return ResponseEntity.ok(taskService.addComment(teamId, taskId, req.getText()));
    }

    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId
    ) {
        taskService.deleteComment(teamId, taskId, commentId);
        return ResponseEntity.noContent().build();
    }
}

