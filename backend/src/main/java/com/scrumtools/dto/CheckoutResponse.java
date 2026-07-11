package com.scrumtools.dto;

import java.util.UUID;

public record CheckoutResponse(
        UUID transactionId,
        String paymentUrl
) {}
