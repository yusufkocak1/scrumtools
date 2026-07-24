package com.scrumtools.entity.enums;

/**
 * Org seviyesi CI/CD bağlantısının durumu.
 * INVALID: kimlik bilgileri reddedildi ya da sunucuya ardışık olarak ulaşılamadı —
 * org admin bağlantıyı gözden geçirmeli. Poller INVALID bağlantıları atlar.
 */
public enum CiConnectionStatus {
    ACTIVE,
    INVALID,
    DISABLED
}
