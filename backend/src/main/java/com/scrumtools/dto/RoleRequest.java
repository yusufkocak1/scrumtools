package com.scrumtools.dto;

import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.RoleScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record RoleRequest(
        @NotBlank(message = "Rol adı boş olamaz")
        String name,

        String description,

        @NotNull(message = "Rol kapsamı boş olamaz")
        RoleScope scope,

        UUID scopeId,

        Set<Permission> permissions,

        String color
) {}

