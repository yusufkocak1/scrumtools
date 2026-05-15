package com.scrumtools.dto;

import com.scrumtools.entity.enums.InvitationStatus;
import com.scrumtools.entity.enums.InvitationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        InvitationType type,
        UUID targetId,
        UUID roleId,
        String roleName,
        String token,
        InvitationStatus status,
        String invitedByName,
        String invitedByEmail,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt
) {}

