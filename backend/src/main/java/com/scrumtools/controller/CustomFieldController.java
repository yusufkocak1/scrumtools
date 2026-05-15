package com.scrumtools.controller;

import com.scrumtools.dto.CustomFieldDefinitionRequest;
import com.scrumtools.dto.CustomFieldDefinitionResponse;
import com.scrumtools.service.CustomFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomFieldController {

    private final CustomFieldService customFieldService;

    // ─── Team bazlı ───────────────────────────────────────────────────────────

    @GetMapping("/api/teams/{teamId}/custom-fields")
    public ResponseEntity<List<CustomFieldDefinitionResponse>> getByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(customFieldService.getFieldsByTeam(teamId));
    }

    @PostMapping("/api/teams/{teamId}/custom-fields")
    public ResponseEntity<CustomFieldDefinitionResponse> createForTeam(
            @PathVariable UUID teamId,
            @RequestBody CustomFieldDefinitionRequest request
    ) {
        return ResponseEntity.ok(customFieldService.createForTeam(teamId, request));
    }

    // ─── Proje bazlı ──────────────────────────────────────────────────────────

    @GetMapping("/api/projects/{projectId}/custom-fields")
    public ResponseEntity<List<CustomFieldDefinitionResponse>> getByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(customFieldService.getFieldsByProject(projectId));
    }

    @PostMapping("/api/projects/{projectId}/custom-fields")
    public ResponseEntity<CustomFieldDefinitionResponse> createForProject(
            @PathVariable UUID projectId,
            @RequestBody CustomFieldDefinitionRequest request
    ) {
        return ResponseEntity.ok(customFieldService.createForProject(projectId, request));
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @PutMapping("/api/custom-fields/{fieldId}")
    public ResponseEntity<CustomFieldDefinitionResponse> update(
            @PathVariable UUID fieldId,
            @RequestBody CustomFieldDefinitionRequest request
    ) {
        return ResponseEntity.ok(customFieldService.update(fieldId, request));
    }

    @DeleteMapping("/api/custom-fields/{fieldId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fieldId) {
        customFieldService.delete(fieldId);
        return ResponseEntity.noContent().build();
    }
}

