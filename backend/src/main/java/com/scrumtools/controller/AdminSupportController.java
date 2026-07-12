package com.scrumtools.controller;

import com.scrumtools.dto.SupportMessageRequest;
import com.scrumtools.dto.SupportMessageResponse;
import com.scrumtools.dto.SupportTicketDetailResponse;
import com.scrumtools.dto.SupportTicketResponse;
import com.scrumtools.entity.enums.SupportCategory;
import com.scrumtools.entity.enums.SupportTicketStatus;
import com.scrumtools.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Destek taleplerinin admin panelinden yönetimi (sadece SUPER_ADMIN).
 */
@RestController
@RequestMapping("/api/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    public ResponseEntity<Page<SupportTicketResponse>> list(
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportCategory category,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(supportTicketService.getTicketsForAdmin(status, category, organizationId, page, size));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<SupportTicketDetailResponse> detail(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(supportTicketService.getTicketDetailForAdmin(ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<SupportMessageResponse> reply(
            @PathVariable UUID ticketId,
            @RequestParam(defaultValue = "true") boolean setWaitingUser,
            @Valid @RequestBody SupportMessageRequest request) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(supportTicketService.addAdminReply(adminEmail, ticketId, request, setWaitingUser));
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @PathVariable UUID ticketId,
            @RequestParam SupportTicketStatus status) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(supportTicketService.updateStatus(adminEmail, ticketId, status));
    }
}
