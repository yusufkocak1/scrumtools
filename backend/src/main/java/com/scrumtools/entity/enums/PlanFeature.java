package com.scrumtools.entity.enums;

/**
 * Paket bazlı açılıp kapatılabilen uygulama modülleri.
 * Plan.features içinde saklanır; EntitlementService.assertFeature ile denetlenir.
 */
public enum PlanFeature {
    SCRUM_POKER,
    RETRO,
    WORK_BOARD,
    CODE_SHARE,
    QUIZ,
    DOCS,
    DASHBOARD_REPORTS,
    ATTACHMENTS,
    CUSTOM_ROLES,
    GIT_INTEGRATION
}
