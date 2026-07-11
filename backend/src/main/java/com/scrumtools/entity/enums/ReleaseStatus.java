package com.scrumtools.entity.enums;

/**
 * Release yaşam döngüsü:
 * OPEN → CODE_FREEZE (paket kapandı) → REGRESSION → APPROVED → RELEASED (terminal)
 * CANCELLED her aşamadan girilebilir, CANCELLED → OPEN ile yeniden açılabilir.
 */
public enum ReleaseStatus {
    OPEN,
    CODE_FREEZE,
    REGRESSION,
    APPROVED,
    RELEASED,
    CANCELLED
}
