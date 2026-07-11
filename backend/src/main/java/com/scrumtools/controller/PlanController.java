package com.scrumtools.controller;

import com.scrumtools.dto.PlanResponse;
import com.scrumtools.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Müşteri arayüzündeki plan kartları için paket listesi.
 * Authenticated tüm kullanıcılar erişebilir; sadece aktif + public planlar döner.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getPublicPlans() {
        return ResponseEntity.ok(planService.getPublicPlans());
    }
}
