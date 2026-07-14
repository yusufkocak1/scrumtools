package com.scrumtools.entity.enums;

/**
 * Task'a bağlı branch'in yaşam döngüsü durumu.
 * Webhook delete event'i ve günlük reconcile ile güncellenir.
 */
public enum ScmBranchStatus {
    ACTIVE,
    MERGED,
    DELETED
}
