package com.scrumtools.controller;

import com.scrumtools.dto.UserProfileResponse;
import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.entity.enums.UserStatus;
import com.scrumtools.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminController {

    private final UserProfileService userProfileService;

    /**
     * Tüm kullanıcıları listele (sadece SUPER_ADMIN)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userProfileService.getAllUsers());
    }

    /**
     * Kullanıcı durumunu değiştir (sadece SUPER_ADMIN)
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(userProfileService.updateUserStatus(userId, status));
    }

    /**
     * Sistem rolünü değiştir (sadece SUPER_ADMIN)
     */
    @PutMapping("/users/{userId}/system-role")
    public ResponseEntity<UserProfileResponse> updateSystemRole(
            @PathVariable UUID userId,
            @RequestParam SystemRole role) {
        return ResponseEntity.ok(userProfileService.updateSystemRole(userId, role));
    }
}

