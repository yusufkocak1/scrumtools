package com.scrumtools.controller;

import com.scrumtools.dto.ScmRepoMappingRequest;
import com.scrumtools.dto.ScmRepositoryResponse;
import com.scrumtools.service.scm.ScmRepoMappingService;
import com.scrumtools.service.scm.client.ScmBranchInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Repo–proje eşleme yönetimi. Yazma işlemleri PROJECT_MANAGE_SETTINGS,
 * görüntüleme proje üyeliği ister (kontroller servis katmanında).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/scm/repos")
@RequiredArgsConstructor
public class ScmRepositoryController {

    private final ScmRepoMappingService mappingService;

    @GetMapping
    public ResponseEntity<List<ScmRepositoryResponse>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(mappingService.listByProject(projectId, currentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<ScmRepositoryResponse> map(
            @PathVariable UUID projectId,
            @RequestBody ScmRepoMappingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mappingService.map(projectId, currentUserEmail(),
                        request.connectionId(), request.externalId()));
    }

    @DeleteMapping("/{repoId}")
    public ResponseEntity<Void> unmap(
            @PathVariable UUID projectId,
            @PathVariable UUID repoId
    ) {
        mappingService.unmap(projectId, currentUserEmail(), repoId);
        return ResponseEntity.noContent().build();
    }

    /** FAILED durumundaki webhook'u yeniden kurmayı dener. */
    @PostMapping("/{repoId}/rewebhook")
    public ResponseEntity<ScmRepositoryResponse> rewebhook(
            @PathVariable UUID projectId,
            @PathVariable UUID repoId
    ) {
        return ResponseEntity.ok(mappingService.rewebhook(projectId, currentUserEmail(), repoId));
    }

    /** Canlı branch listesi — branch açarken base seçimi için. */
    @GetMapping("/{repoId}/branches")
    public ResponseEntity<List<ScmBranchInfo>> listBranches(
            @PathVariable UUID projectId,
            @PathVariable UUID repoId,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(mappingService.listRemoteBranches(
                projectId, currentUserEmail(), repoId, search));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
