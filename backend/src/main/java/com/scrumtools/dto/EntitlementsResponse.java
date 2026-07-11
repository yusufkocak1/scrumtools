package com.scrumtools.dto;

import com.scrumtools.entity.enums.PlanFeature;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Bir organizasyonun o anki efektif paket hakları.
 * status: TRIAL | ACTIVE | PAST_DUE | FREE (canlı abonelik yoksa FREE)
 */
public record EntitlementsResponse(
        String planCode,
        String planName,
        String status,
        LocalDateTime trialEndsAt,
        LocalDateTime periodEnd,
        Integer maxMembers,
        Integer maxProjects,
        Set<PlanFeature> features,
        long memberCount,
        long projectCount
) {}
