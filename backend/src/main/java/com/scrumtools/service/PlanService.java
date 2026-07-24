package com.scrumtools.service;

import com.scrumtools.dto.PlanRequest;
import com.scrumtools.dto.PlanResponse;
import com.scrumtools.entity.Plan;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;

    /** Müşteri arayüzündeki plan kartları için: aktif + public planlar. */
    public List<PlanResponse> getPublicPlans() {
        return planRepository.findByActiveTrueAndIsPublicTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public Plan getByCode(String code) {
        return planRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Plan bulunamadı: " + code));
    }

    /** Canlı aboneliği olmayan org'ların düştüğü varsayılan (FREE) plan. */
    public Plan getDefaultPlan() {
        return planRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("Varsayılan (FREE) plan tanımlı değil. Seed çalışmamış olabilir."));
    }

    /** Trial'ı temsil eden üst paket: trialDays tanımlı, aktif en yüksek sortOrder'lı plan. */
    public Plan getTrialPlan() {
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()) && p.getTrialDays() != null && p.getTrialDays() > 0)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("Trial planı tanımlı değil. Seed çalışmamış olabilir."));
    }

    // ─── Superadmin plan yönetimi ─────────────────────────────────────────────

    /** Tüm planlar (pasif ve gizli olanlar dahil) — superadmin paneli için. */
    public List<PlanResponse> getAllPlansAdmin() {
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        String code = request.code().toUpperCase().trim();
        if (planRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Bu plan kodu zaten mevcut: " + code);
        }
        Plan plan = planRepository.save(Plan.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description())
                .maxMembers(request.maxMembers())
                .maxProjects(request.maxProjects())
                .features(request.features() != null ? request.features() : Set.of())
                .monthlyPriceTry(request.monthlyPriceTry())
                .yearlyPriceTry(request.yearlyPriceTry())
                .trialDays(request.trialDays())
                .isDefault(false)
                .isPublic(request.isPublic() == null || request.isPublic())
                .active(request.active() == null || request.active())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 99)
                .build());
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(java.util.UUID planId, PlanRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan bulunamadı: " + planId));

        // Plan kodu Organization.plan cache'inde ve abonelik geçmişinde referans alınır — değiştirilemez
        if (!plan.getCode().equalsIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Plan kodu değiştirilemez (mevcut aboneliklerde referans alınıyor).");
        }
        if (Boolean.TRUE.equals(plan.getIsDefault()) && Boolean.FALSE.equals(request.active())) {
            throw new IllegalArgumentException("Varsayılan (FREE) plan pasifleştirilemez.");
        }

        plan.setName(request.name().trim());
        plan.setDescription(request.description());
        plan.setMaxMembers(request.maxMembers());
        plan.setMaxProjects(request.maxProjects());
        if (request.features() != null) plan.setFeatures(request.features());
        plan.setMonthlyPriceTry(request.monthlyPriceTry());
        plan.setYearlyPriceTry(request.yearlyPriceTry());
        plan.setTrialDays(request.trialDays());
        if (request.isPublic() != null) plan.setIsPublic(request.isPublic());
        if (request.active() != null) plan.setActive(request.active());
        if (request.sortOrder() != null) plan.setSortOrder(request.sortOrder());

        return toResponse(planRepository.save(plan));
    }

    /**
     * Varsayılan paketleri oluşturur (code'a göre idempotent).
     * Fiyat/limit/özellikler DB üzerinden superadmin panelinden düzenlenebilir;
     * buradaki değerler sadece ilk kurulum içindir.
     */
    @Transactional
    public void seedDefaultPlans() {
        seedPlan("FREE", "Free", "Küçük ekipler için ücretsiz başlangıç paketi.",
                5, 1,
                EnumSet.of(PlanFeature.SCRUM_POKER, PlanFeature.RETRO, PlanFeature.WORK_BOARD),
                BigDecimal.ZERO, BigDecimal.ZERO,
                null, true, 0);

        seedPlan("PRO", "Pro", "Büyüyen ekipler için tüm temel modüller.",
                25, 10,
                EnumSet.of(PlanFeature.SCRUM_POKER, PlanFeature.RETRO, PlanFeature.WORK_BOARD,
                        PlanFeature.CODE_SHARE, PlanFeature.QUIZ, PlanFeature.DOCS,
                        PlanFeature.DASHBOARD_REPORTS, PlanFeature.ATTACHMENTS,
                        PlanFeature.GIT_INTEGRATION, PlanFeature.CI_CD_INTEGRATION),
                new BigDecimal("499.00"), new BigDecimal("4990.00"),
                null, false, 1);

        seedPlan("MAX", "Max", "Sınırsız üye ve proje, tüm özellikler.",
                null, null,
                EnumSet.allOf(PlanFeature.class),
                new BigDecimal("999.00"), new BigDecimal("9990.00"),
                14, false, 2);
    }

    private void seedPlan(String code, String name, String description,
                          Integer maxMembers, Integer maxProjects, Set<PlanFeature> features,
                          BigDecimal monthly, BigDecimal yearly,
                          Integer trialDays, boolean isDefault, int sortOrder) {
        if (planRepository.existsByCode(code)) return;
        planRepository.save(Plan.builder()
                .code(code)
                .name(name)
                .description(description)
                .maxMembers(maxMembers)
                .maxProjects(maxProjects)
                .features(features)
                .monthlyPriceTry(monthly)
                .yearlyPriceTry(yearly)
                .trialDays(trialDays)
                .isDefault(isDefault)
                .isPublic(true)
                .active(true)
                .sortOrder(sortOrder)
                .build());
        log.info("Plan seed edildi: {}", code);
    }

    public PlanResponse toResponse(Plan p) {
        return new PlanResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getMaxMembers(),
                p.getMaxProjects(),
                p.getFeatures(),
                p.getMonthlyPriceTry(),
                p.getYearlyPriceTry(),
                p.getTrialDays(),
                p.getIsDefault(),
                p.getIsPublic(),
                p.getActive(),
                p.getSortOrder()
        );
    }
}
