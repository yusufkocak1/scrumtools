package com.scrumtools.entity.enums;

/**
 * Abonelik yaşam döngüsü durumları.
 * Org başına en fazla bir canlı (TRIAL/ACTIVE/PAST_DUE) abonelik olabilir;
 * FREE paket, canlı aboneliğin YOKLUĞU ile temsil edilir (satır yok).
 */
public enum SubscriptionStatus {
    TRIAL,      // Deneme süresi devam ediyor
    ACTIVE,     // Ödenmiş, dönem devam ediyor
    PAST_DUE,   // Dönem bitti, grace period içinde (entitlement devam eder)
    EXPIRED,    // Süresi doldu, FREE'ye düşürüldü
    CANCELED    // Kullanıcı/admin tarafından iptal edildi
}
