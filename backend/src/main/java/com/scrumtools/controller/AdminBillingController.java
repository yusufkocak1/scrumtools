package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.AdminBillingService;
import com.scrumtools.service.BillingService;
import com.scrumtools.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Superadmin müşteri yönetim paneli API'si.
 * Tüm endpoint'ler class-level @PreAuthorize ile SUPER_ADMIN'e kilitlidir.
 */
@RestController
@RequestMapping("/api/admin/billing")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminBillingController {

    private final AdminBillingService adminBillingService;
    private final BillingService billingService;
    private final PlanService planService;
    private final UserRepository userRepository;

    // ─── Müşteriler ───────────────────────────────────────────────────────────

    @GetMapping("/organizations")
    public ResponseEntity<List<AdminOrgBillingResponse>> listOrganizations() {
        return ResponseEntity.ok(adminBillingService.listOrganizations());
    }

    /** action: ACTIVATE | EXTEND | CHANGE_PLAN | EXTEND_TRIAL | CANCEL */
    @PostMapping("/organizations/{orgId}/subscription")
    public ResponseEntity<AdminOrgBillingResponse> applySubscriptionAction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody AdminSubscriptionActionRequest request) {
        return ResponseEntity.ok(
                adminBillingService.applySubscriptionAction(orgId, request, userDetails.getUsername()));
    }

    /** Ödeme linki oluştur + org sahibine e-posta ile gönder. */
    @PostMapping("/organizations/{orgId}/payment-link")
    public ResponseEntity<PaymentTransactionResponse> createPaymentLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orgId,
            @Valid @RequestBody CheckoutRequest request) {
        User actor = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı."));
        BillingCycle cycle = request.cycle() != null ? request.cycle() : BillingCycle.MONTHLY;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.adminCreatePaymentLink(orgId, request.planCode(), cycle, actor));
    }

    @GetMapping("/organizations/{orgId}/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getTransactions(@PathVariable UUID orgId) {
        return ResponseEntity.ok(billingService.getTransactionsInternal(orgId));
    }

    @PutMapping("/organizations/{orgId}/suspend")
    public ResponseEntity<AdminOrgBillingResponse> setSuspended(
            @PathVariable UUID orgId,
            @RequestParam boolean suspended) {
        return ResponseEntity.ok(adminBillingService.setSuspended(orgId, suspended));
    }

    // ─── Planlar ──────────────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlansAdmin());
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(request));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<PlanResponse> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(planId, request));
    }
}
