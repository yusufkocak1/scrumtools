package com.scrumtools.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Superadmin müşteri paneli satırı. */
public record AdminOrgBillingResponse(
        UUID orgId,
        String orgName,
        String slug,
        String ownerName,
        String ownerEmail,
        String planCode,
        String subscriptionStatus,   // TRIAL | ACTIVE | PAST_DUE | FREE
        LocalDateTime trialEndsAt,
        LocalDateTime currentPeriodEnd,
        long memberCount,
        boolean suspended,
        LocalDateTime lastPaymentAt,
        BigDecimal lastPaymentAmount,
        LocalDateTime createdAt
) {}
