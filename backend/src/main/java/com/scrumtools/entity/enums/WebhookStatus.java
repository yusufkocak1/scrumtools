package com.scrumtools.entity.enums;

/**
 * Eşlenmiş repoya webhook kurulum durumu.
 * NONE/FAILED durumundaki repolar için ScmSyncScheduler poller'ı devreye girer.
 */
public enum WebhookStatus {
    ACTIVE,
    FAILED,
    NONE
}
