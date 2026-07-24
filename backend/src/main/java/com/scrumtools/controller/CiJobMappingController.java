package com.scrumtools.controller;

import com.scrumtools.dto.CiJobMappingRequest;
import com.scrumtools.dto.CiJobMappingResponse;
import com.scrumtools.service.ci.CiJobMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Job–proje eşleme yönetimi. Yazma işlemleri PROJECT_MANAGE_SETTINGS,
 * görüntüleme proje üyeliği ister (kontroller servis katmanında).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/ci/job-mappings")
@RequiredArgsConstructor
public class CiJobMappingController {

    private final CiJobMappingService jobMappingService;

    @GetMapping
    public ResponseEntity<List<CiJobMappingResponse>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(jobMappingService.listByProject(projectId, currentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<CiJobMappingResponse> create(
            @PathVariable UUID projectId,
            @RequestBody CiJobMappingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobMappingService.create(projectId, currentUserEmail(), request));
    }

    @PutMapping("/{mappingId}")
    public ResponseEntity<CiJobMappingResponse> update(
            @PathVariable UUID projectId,
            @PathVariable UUID mappingId,
            @RequestBody CiJobMappingRequest request
    ) {
        return ResponseEntity.ok(jobMappingService.update(projectId, currentUserEmail(), mappingId, request));
    }

    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID mappingId
    ) {
        jobMappingService.delete(projectId, currentUserEmail(), mappingId);
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
