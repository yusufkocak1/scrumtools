package com.scrumtools.dto;

import com.scrumtools.entity.enums.OrgRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrgMemberResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        String userAvatarUrl,
        OrgRole orgRole,
        LocalDateTime joinedAt
) {}

