package com.scrumtools.controller;

import com.scrumtools.dto.ScmConnectionRequest;
import com.scrumtools.dto.ScmConnectionResponse;
import com.scrumtools.dto.ScmConnectionTestResponse;
import com.scrumtools.dto.ScmRemoteRepoResponse;
import com.scrumtools.service.scm.ScmConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Org seviyesi SCM bağlantı yönetimi — sadece ORG_OWNER/ORG_ADMIN
 * (kontrol servis katmanında). Token hiçbir yanıtta yer almaz.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/scm/connections")
@RequiredArgsConstructor
public class ScmConnectionController {

    private final ScmConnectionService connectionService;

    @GetMapping
    public ResponseEntity<List<ScmConnectionResponse>> list(@PathVariable UUID orgId) {
        return ResponseEntity.ok(connectionService.list(orgId, currentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<ScmConnectionResponse> create(
            @PathVariable UUID orgId,
            @RequestBody ScmConnectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(connectionService.create(orgId, currentUserEmail(), request));
    }

    @PutMapping("/{connectionId}")
    public ResponseEntity<ScmConnectionResponse> update(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId,
            @RequestBody ScmConnectionRequest request
    ) {
        return ResponseEntity.ok(connectionService.update(orgId, currentUserEmail(), connectionId, request));
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId
    ) {
        connectionService.delete(orgId, currentUserEmail(), connectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{connectionId}/test")
    public ResponseEntity<ScmConnectionTestResponse> test(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId
    ) {
        return ResponseEntity.ok(connectionService.test(orgId, currentUserEmail(), connectionId));
    }

    /** Sağlayıcıdan canlı repo listesi — eşleme ekranındaki arama için. */
    @GetMapping("/{connectionId}/repos")
    public ResponseEntity<List<ScmRemoteRepoResponse>> listRemoteRepos(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(connectionService.listRemoteRepos(
                orgId, currentUserEmail(), connectionId, search, page));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
