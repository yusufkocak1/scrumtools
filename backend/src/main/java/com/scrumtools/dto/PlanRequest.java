package com.scrumtools.dto;

import com.scrumtools.entity.enums.PlanFeature;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Set;

/** Superadmin plan oluşturma/güncelleme isteği. */
public record PlanRequest(
        @NotBlank(message = "Plan kodu boş olamaz")
        String code,

        @NotBlank(message = "Plan adı boş olamaz")
        String name,

        String description,
        Integer maxMembers,
        Integer maxProjects,
        Set<PlanFeature> features,
        BigDecimal monthlyPriceTry,
        BigDecimal yearlyPriceTry,
        Integer trialDays,
        Boolean isPublic,
        Boolean active,
        Integer sortOrder
) {}
