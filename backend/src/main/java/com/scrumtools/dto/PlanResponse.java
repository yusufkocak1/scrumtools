package com.scrumtools.dto;

import com.scrumtools.entity.enums.PlanFeature;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        Integer maxMembers,
        Integer maxProjects,
        Set<PlanFeature> features,
        BigDecimal monthlyPriceTry,
        BigDecimal yearlyPriceTry,
        Integer trialDays,
        Boolean isDefault,
        Boolean isPublic,
        Boolean active,
        Integer sortOrder
) {}
