package com.scrumtools.dto;

import com.scrumtools.entity.enums.ProjectStatus;
import com.scrumtools.entity.enums.ProjectType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String key,
        String description,
        UUID organizationId,
        String organizationName,
        UUID leadId,
        String leadName,
        ProjectType projectType,
        String iconUrl,
        String color,
        ProjectStatus status,
        int memberCount,
        LocalDateTime createdAt
) {}

