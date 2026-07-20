package com.scrumtools.controller;

import com.scrumtools.dto.TeamRequest;
import com.scrumtools.dto.TeamResponse;
import com.scrumtools.dto.UpdateMemberRoleRequest;
import com.scrumtools.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * GET /api/teams — Kullanıcının üye olduğu takımlar
     */
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(Authentication authentication) {
        return ResponseEntity.ok(teamService.getMyTeams(authentication.getName()));
    }

    /**
     * GET /api/teams/{id} — Takım detayı
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    /**
     * PUT /api/teams/{id} — Takım adını ve kodunu güncelle (sadece takım admini)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody TeamRequest request
    ) {
        return ResponseEntity.ok(teamService.updateTeam(id, request, authentication.getName()));
    }

    /**
     * PUT /api/teams/{id}/members/{email}/role — Üye rolünü ve skill'lerini güncelle
     */
    @PutMapping("/{id}/members/{email}/role")
    public ResponseEntity<TeamResponse> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable String email,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return ResponseEntity.ok(teamService.updateMemberRole(id, email, request));
    }

    /**
     * DELETE /api/teams/{id}/members/{email} — Üyeyi takımdan çıkar
     */
    @DeleteMapping("/{id}/members/{email}")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable UUID id,
            @PathVariable String email
    ) {
        return ResponseEntity.ok(teamService.removeUserFromTeam(id, email));
    }

    /**
     * PUT /api/teams/{id}/project — Takımın birincil (varsayılan) projesini ayarla
     * (projectId=null → birincil proje bağlantısını kaldır). Proje aynı zamanda takımın
     * çalıştığı projeler kümesine eklenir.
     * Yetki: takım admini veya org admin/owner.
     */
    @PutMapping("/{id}/project")
    public ResponseEntity<TeamResponse> linkProject(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody java.util.Map<String, UUID> body
    ) {
        return ResponseEntity.ok(teamService.linkProject(id, body.get("projectId"), authentication.getName()));
    }

    /**
     * POST /api/teams/{id}/projects — Takımın çalıştığı projelere yeni proje ekle.
     * Bir takım aynı anda birden fazla projede çalışabilir; ilk eklenen proje birincil olur.
     */
    @PostMapping("/{id}/projects")
    public ResponseEntity<TeamResponse> addProject(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody java.util.Map<String, UUID> body
    ) {
        return ResponseEntity.ok(teamService.addProject(id, body.get("projectId"), authentication.getName()));
    }

    /**
     * DELETE /api/teams/{id}/projects/{projectId} — Takımı projeden ayır.
     * Takımın o projede görevi varsa reddedilir (görevler önce taşınmalı).
     */
    @DeleteMapping("/{id}/projects/{projectId}")
    public ResponseEntity<TeamResponse> removeProject(
            @PathVariable UUID id,
            @PathVariable UUID projectId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teamService.removeProject(id, projectId, authentication.getName()));
    }

    /**
     * PUT /api/teams/display-name — Tüm takımlardaki display name'i güncelle
     */
    @PutMapping("/display-name")
    public ResponseEntity<Void> updateDisplayName(
            Authentication authentication,
            @RequestBody java.util.Map<String, String> body
    ) {
        String newName = body.get("displayName");
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("displayName boş olamaz");
        }
        teamService.updateDisplayNameAcrossTeams(authentication.getName(), newName.trim());
        return ResponseEntity.ok().build();
    }
}
