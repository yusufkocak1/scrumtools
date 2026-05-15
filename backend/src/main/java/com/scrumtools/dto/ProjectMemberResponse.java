package com.scrumtools.dto;

import com.scrumtools.entity.enums.MemberType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        String userAvatarUrl,
        List<RoleInfo> roles,
        LocalDateTime joinedAt,
        MemberType memberType
) {
    public record RoleInfo(UUID id, String name, String color) {}
}

