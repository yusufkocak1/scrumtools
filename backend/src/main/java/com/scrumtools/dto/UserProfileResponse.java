package com.scrumtools.dto;

import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        String phone,
        String timezone,
        String locale,
        SystemRole systemRole,
        UserStatus status,
        Boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}

