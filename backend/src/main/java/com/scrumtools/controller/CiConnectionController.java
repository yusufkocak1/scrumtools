package com.scrumtools.controller;

import com.scrumtools.dto.CiConnectionRequest;
import com.scrumtools.dto.CiConnectionResponse;
import com.scrumtools.dto.CiConnectionTestResponse;
import com.scrumtools.dto.CiJobInfoResponse;
import com.scrumtools.dto.CiJobParameterResponse;
import com.scrumtools.service.ci.CiConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Org seviyesi CI/CD bağlantı yönetimi — sadece ORG_OWNER/ORG_ADMIN
 * (kontrol servis katmanında). Token hiçbir yanıtta yer almaz.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/ci/connections")
@RequiredArgsConstructor
public class CiConnectionController {

    private final CiConnectionService connectionService;

    @GetMapping
    public ResponseEntity<List<CiConnectionResponse>> list(@PathVariable UUID orgId) {
        return ResponseEntity.ok(connectionService.list(orgId, currentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<CiConnectionResponse> create(
            @PathVariable UUID orgId,
            @RequestBody CiConnectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(connectionService.create(orgId, currentUserEmail(), request));
    }

    @PutMapping("/{connectionId}")
    public ResponseEntity<CiConnectionResponse> update(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId,
            @RequestBody CiConnectionRequest request
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
    public ResponseEntity<CiConnectionTestResponse> test(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId
    ) {
        return ResponseEntity.ok(connectionService.test(orgId, currentUserEmail(), connectionId));
    }

    /** Sunucudan canlı job listesi — eşleme ekranındaki arama için. */
    @GetMapping("/{connectionId}/jobs")
    public ResponseEntity<List<CiJobInfoResponse>> listJobs(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(connectionService.listJobs(orgId, currentUserEmail(), connectionId, search));
    }

    /** Seçilen job'ın parametre tanımları — şablon editörünü ön doldurmak için. */
    @GetMapping("/{connectionId}/jobs/params")
    public ResponseEntity<List<CiJobParameterResponse>> listJobParameters(
            @PathVariable UUID orgId,
            @PathVariable UUID connectionId,
            @RequestParam String jobFullName
    ) {
        return ResponseEntity.ok(connectionService.listJobParameters(
                orgId, currentUserEmail(), connectionId, jobFullName));
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
