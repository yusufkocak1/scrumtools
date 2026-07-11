package com.scrumtools.entity.enums;

/**
 * Aboneliğin nasıl başlatıldığı/aktive edildiği.
 */
public enum SubscriptionSource {
    TRIAL,      // Org oluşturulurken otomatik başlayan deneme
    IYZILINK,   // iyzico ödeme linki üzerinden otomatik aktivasyon
    MANUAL      // Superadmin panelinden manuel aktivasyon (havale/EFT vb.)
}
