package com.scrumtools.entity.enums;

/**
 * Paket bazlı açılıp kapatılabilen uygulama modülleri.
 * Plan.features içinde saklanır; EntitlementService.assertFeature ile denetlenir.
 */
public enum PlanFeature {
    SCRUM_POKER,
    RETRO,
    WORK_BOARD,
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
    RICH_FILTERS,

    /**
     * Eş zamanlı ortak çalışma alanı — metin ve kod dokümanları (COLLAB_WORKSPACE_PLAN.md).
     *
     * Kaldırılan {@code CODE_SHARE} özelliğinin yerini alır; mevcut kurulumlarda
     * {@code CollabCleanupRunner} eski satırları bu değere göç ettirir (K10).
     * Hesap tablosu ({@code COLLAB_SHEET}) ve makrolar ({@code COLLAB_MACRO})
     * ayrı özelliklerdir, sırasıyla Faz 3 ve Faz 4'te eklenir.
     */
    COLLAB_WORKSPACE
}
