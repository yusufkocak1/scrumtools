package com.scrumtools.dto;

import com.scrumtools.entity.enums.OrgPlan;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String logoUrl,
        UUID ownerId,
        String ownerName,
        OrgPlan plan,
        Integer maxMembers,
        int memberCount,
        LocalDateTime createdAt
) {}

