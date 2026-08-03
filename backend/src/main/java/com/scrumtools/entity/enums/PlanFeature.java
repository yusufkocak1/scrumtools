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
    GIT_INTEGRATION,
    CI_CD_INTEGRATION,

    /**
     * Zengin filtreler ve onlara bağlı dashboard widget'ları.
     *
     * Yalnız <b>oluşturma ve düzenleme</b> bu özelliğe bağlıdır; paylaşılan bir
     * zengin filtreyi görüntülemek, grafiklerini açmak ve {@code smart[…]} ile
     * sorgulamak her pakette serbesttir — paylaşılan bir dashboard'un takımın
     * FREE üyelerinde bozuk görünmesi, özelliğin yayılmasını engellerdi.
     * Bkz. RICH_FILTER_PLAN.md — K16.
     */
    RICH_FILTERS
}
