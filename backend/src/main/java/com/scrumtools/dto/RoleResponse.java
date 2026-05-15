package com.scrumtools.dto;

import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.RoleScope;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        RoleScope scope,
        UUID scopeId,
        Boolean isDefault,
        Set<Permission> permissions,
        String color,
        LocalDateTime createdAt
) {}

