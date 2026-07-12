package com.scrumtools.controller;

import com.scrumtools.dto.ErrorEventResponse;
import com.scrumtools.dto.ErrorGroupDetailResponse;
import com.scrumtools.dto.ErrorGroupResponse;
import com.scrumtools.entity.enums.ErrorGroupStatus;
import com.scrumtools.service.ErrorTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Hata gruplarının admin panelinden yönetimi (sadece SUPER_ADMIN).
 */
@RestController
@RequestMapping("/api/admin/errors")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminErrorController {

    private final ErrorTrackingService errorTrackingService;

    @GetMapping("/groups")
    public ResponseEntity<Page<ErrorGroupResponse>> getGroups(
            @RequestParam(required = false) ErrorGroupStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(errorTrackingService.getGroups(status, page, size));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ErrorGroupDetailResponse> getGroupDetail(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(errorTrackingService.getGroupDetail(groupId, page, size));
    }

    @PutMapping("/groups/{groupId}/status")
    public ResponseEntity<ErrorGroupResponse> updateGroupStatus(
            @PathVariable UUID groupId,
            @RequestParam ErrorGroupStatus status) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(errorTrackingService.updateGroupStatus(groupId, status, adminEmail));
    }

    @GetMapping("/events/by-code/{trackingCode}")
    public ResponseEntity<ErrorEventResponse> getEventByTrackingCode(@PathVariable String trackingCode) {
        return ResponseEntity.ok(errorTrackingService.getEventByTrackingCode(trackingCode));
    }
}
