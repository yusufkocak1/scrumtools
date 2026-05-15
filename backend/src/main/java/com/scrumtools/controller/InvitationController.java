package com.scrumtools.controller;

import com.scrumtools.dto.InvitationRequest;
import com.scrumtools.dto.InvitationResponse;
import com.scrumtools.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> sendInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.sendInvitation(userDetails.getUsername(), request));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(invitationService.getPendingInvitations(userDetails.getUsername()));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String token) {
        return ResponseEntity.ok(invitationService.acceptInvitation(token, userDetails.getUsername()));
    }

    @PostMapping("/{token}/decline")
    public ResponseEntity<InvitationResponse> declineInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String token) {
        return ResponseEntity.ok(invitationService.declineInvitation(token, userDetails.getUsername()));
    }
}

