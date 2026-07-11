package com.scrumtools.dto;

import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentTransactionResponse(
        UUID id,
        String planCode,
        String planName,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paymentUrl,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {}
