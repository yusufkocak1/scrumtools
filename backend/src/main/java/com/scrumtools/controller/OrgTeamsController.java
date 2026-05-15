package com.scrumtools.controller;

import com.scrumtools.dto.TeamRequest;
import com.scrumtools.dto.TeamResponse;
import com.scrumtools.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Organizasyon kapsamındaki takım işlemleri.
 * Tüm endpoint'ler /api/organizations/{orgId}/teams altında yer alır.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/teams")
@RequiredArgsConstructor
public class OrgTeamsController {

    private final TeamService teamService;

    /**
     * GET /api/organizations/{orgId}/teams?q= — Org'a ait takımları listele / ara
     */
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeamsByOrg(
            @PathVariable UUID orgId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(teamService.getTeamsByOrg(orgId, q));
    }

    /**
     * POST /api/organizations/{orgId}/teams — Takım oluştur (sadece org admin/owner)
     */
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @PathVariable UUID orgId,
            Authentication authentication,
            @Valid @RequestBody TeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(orgId, request, authentication.getName()));
    }

    /**
     * POST /api/organizations/{orgId}/teams/{teamId}/members?email= — Org üyesini takıma ekle.
     * Sadece org admin/owner veya ilgili takımın admini yapabilir.
     * Sadece organizasyon üyeleri eklenebilir.
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable UUID orgId,
            @PathVariable UUID teamId,
            @RequestParam String email,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teamService.addMemberToTeam(orgId, teamId, email, authentication.getName()));
    }
}

