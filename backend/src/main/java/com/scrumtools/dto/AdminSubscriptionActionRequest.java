package com.scrumtools.dto;

import com.scrumtools.entity.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Superadmin abonelik aksiyonları.
 * action: ACTIVATE | EXTEND | CHANGE_PLAN | EXTEND_TRIAL | CANCEL
 */
public record AdminSubscriptionActionRequest(
        @NotBlank(message = "Aksiyon boş olamaz")
        String action,

        String planCode,          // ACTIVATE / CHANGE_PLAN için
        BillingCycle cycle,       // ACTIVATE / EXTEND için (varsayılan MONTHLY)
        LocalDateTime periodEnd,  // Özel dönem sonu (opsiyonel)
        Integer trialDays         // EXTEND_TRIAL için (varsayılan 14)
) {}
