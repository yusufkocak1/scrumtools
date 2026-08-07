package com.scrumtools.entity.enums;

/** Makro çalıştırma sonucu (COLLAB_WORKSPACE_PLAN.md §5 — {@code collab_macro_runs}). */
public enum MacroRunStatus {

    RUNNING,
    SUCCESS,
    FAILED,

    /** Kaynak sınırı aşıldı; worker sonlandırıldı (§9.2). */
    TIMEOUT,

    /**
     * Yetki ya da onay eksikliğinden hiç başlatılmadı.
     *
     * <p>Başarısızlıktan ayrı tutuluyor: reddedilen çalıştırmalar denetim
     * açısından en ilginç kayıtlardır, {@code FAILED} yığınında kaybolmamalı.
     */
    DENIED
}
