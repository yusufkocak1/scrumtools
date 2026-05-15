package com.scrumtools.controller;

import com.scrumtools.dto.SprintRequest;
import com.scrumtools.dto.SprintResponse;
import com.scrumtools.dto.UpdateSprintStatusRequest;
import com.scrumtools.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @GetMapping
    public ResponseEntity<List<SprintResponse>> getSprints(@PathVariable UUID teamId) {
        return ResponseEntity.ok(sprintService.getSprintsByTeam(teamId));
    }

    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable UUID teamId,
            @RequestBody SprintRequest req
    ) {
        return ResponseEntity.ok(sprintService.createSprint(teamId, req));
    }

    @PutMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable UUID teamId,
            @PathVariable UUID sprintId,
            @RequestBody SprintRequest req
    ) {
        return ResponseEntity.ok(sprintService.updateSprint(teamId, sprintId, req));
    }

    @PatchMapping("/{sprintId}/status")
    public ResponseEntity<SprintResponse> updateStatus(
            @PathVariable UUID teamId,
            @PathVariable UUID sprintId,
            @RequestBody UpdateSprintStatusRequest req
    ) {
        return ResponseEntity.ok(sprintService.updateStatus(teamId, sprintId, req.getStatus()));
    }
}
