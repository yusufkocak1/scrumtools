package com.scrumtools.entity.enums;

/**
 * Makro tetikleyicileri (COLLAB_WORKSPACE_PLAN.md §5 / §9.2).
 *
 * <p><b>Otomatik tetikleyiciler onaysız çalışmaz.</b> Manuel çalıştırmada kullanıcı
 * ne yaptığını bilerek düğmeye basar; {@code ON_OPEN} ise dokümanı açan herkesin
 * yetkisiyle sessizce koşar — onay olmadan bu, yetki yükseltme aracıdır.
 */
public enum MacroTriggerType {

    /** Kullanıcı düğmeye basar. */
    MANUAL,

    /** Doküman açıldığında. Onay şart. */
    ON_OPEN,

    /** Doküman değiştiğinde. Onay şart. */
    ON_EDIT,

    /** Zamanlanmış — sunucu tarafı yürütme, Faz 5. */
    ON_SCHEDULE,

    /** Webhook ile — sunucu tarafı yürütme, Faz 5. */
    ON_WEBHOOK
}
