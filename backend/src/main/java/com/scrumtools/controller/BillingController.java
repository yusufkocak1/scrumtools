package com.scrumtools.controller;

import com.scrumtools.dto.CheckoutRequest;
import com.scrumtools.dto.CheckoutResponse;
import com.scrumtools.dto.PaymentTransactionResponse;
import com.scrumtools.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Müşteri (org owner/admin) tarafı ödeme endpoint'leri.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** Satın alma başlat → iyzilink ödeme URL'i döner. */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(billingService.checkout(
                orgId, userDetails.getUsername(), request.planCode(), request.cycle()));
    }

    /** Ödeme geçmişi (BillingTab'daki "ödemeniz bekleniyor" polling'i de bunu kullanır). */
    @GetMapping("/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(billingService.getTransactions(orgId, userDetails.getUsername()));
    }
}
