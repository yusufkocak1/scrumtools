package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.enums.MemberType;
import com.scrumtools.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // Organizasyon altında proje oluştur
    @PostMapping("/api/organizations/{orgId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(orgId, userDetails.getUsername(), request));
    }

    // Organizasyon altındaki projeleri listele
    @GetMapping("/api/organizations/{orgId}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrg(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(projectService.getProjectsByOrg(orgId, userDetails.getUsername()));
    }

    // Proje detayı
    @GetMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId, userDetails.getUsername()));
    }

    // Proje güncelle
    @PutMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(projectId, userDetails.getUsername(), request));
    }

    // Proje sil (soft delete)
    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        projectService.deleteProject(projectId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Proje üyelerini listele
    @GetMapping("/api/projects/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getMembers(projectId, userDetails.getUsername()));
    }

    // Projeye üye ekle
    @PostMapping("/api/projects/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @RequestParam String email,
            @RequestParam(required = false) List<UUID> roleIds,
            @RequestParam(required = false) MemberType memberType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addMember(projectId, userDetails.getUsername(), email, roleIds, memberType));
    }

    // Projeye takımı toplu ekle
    @PostMapping("/api/projects/{projectId}/teams/{teamId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> addTeamToProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @PathVariable UUID teamId,
            @RequestParam(required = false) List<UUID> roleIds,
            @RequestParam(required = false) MemberType memberType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addTeamToProject(projectId, teamId, userDetails.getUsername(), roleIds, memberType));
    }

    // Üye rollerini güncelle
    @PutMapping("/api/projects/{projectId}/members/{userId}/roles")
    public ResponseEntity<ProjectMemberResponse> updateMemberRoles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestBody List<UUID> roleIds) {
        return ResponseEntity.ok(projectService.updateMemberRoles(projectId, userId, userDetails.getUsername(), roleIds));
    }

    // Üyeyi projeden çıkar
    @DeleteMapping("/api/projects/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        projectService.removeMember(projectId, userId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

