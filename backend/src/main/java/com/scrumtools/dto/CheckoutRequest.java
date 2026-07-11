package com.scrumtools.dto;

import com.scrumtools.entity.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank(message = "Plan kodu boş olamaz")
        String planCode,

        @NotNull(message = "Fatura dönemi seçilmeli")
        BillingCycle cycle
) {}
