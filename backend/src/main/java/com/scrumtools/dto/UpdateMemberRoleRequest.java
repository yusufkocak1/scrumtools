package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateMemberRoleRequest(
        @NotBlank(message = "Rol boş olamaz")
        String role,

        List<String> skills
) {}

