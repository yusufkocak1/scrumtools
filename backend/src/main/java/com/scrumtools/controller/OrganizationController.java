package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.service.MemberOnboardingService;
import com.scrumtools.service.OrganizationService;
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
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final MemberOnboardingService memberOnboardingService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganization(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(organizationService.getMyOrganizations(userDetails.getUsername()));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getOrganization(orgId, userDetails.getUsername()));
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, userDetails.getUsername(), request));
    }

    @GetMapping("/{orgId}/entitlements")
    public ResponseEntity<EntitlementsResponse> getEntitlements(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getEntitlements(orgId, userDetails.getUsername()));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<OrgMemberResponse>> getMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getMembers(orgId, userDetails.getUsername()));
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<OrgMemberResponse> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @RequestParam String email,
            @RequestParam(required = false) OrgRole role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMember(orgId, userDetails.getUsername(), email, role));
    }

    /**
     * Üyeyi doğrudan sisteme kaydet (hesabı yoksa oluşturulur, şifre-kurulum maili gider).
     */
    @PostMapping("/{orgId}/members/create")
    public ResponseEntity<OrgMemberResponse> createMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberOnboardingService.createMember(orgId, userDetails.getUsername(), request));
    }

    @PutMapping("/{orgId}/members/{userId}/role")
    public ResponseEntity<OrgMemberResponse> updateMemberRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
            @RequestParam OrgRole role) {
        return ResponseEntity.ok(organizationService.updateMemberRole(orgId, userId, userDetails.getUsername(), role));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @PathVariable UUID userId) {
        organizationService.removeMember(orgId, userId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

