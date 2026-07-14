package com.scrumtools.entity.enums;

/**
 * Org seviyesi SCM bağlantısının durumu.
 * TOKEN_INVALID: sağlayıcı 401/403 döndü — org admin token'ı yenilemeli.
 */
public enum ScmConnectionStatus {
    ACTIVE,
    TOKEN_INVALID,
    DISABLED
}
