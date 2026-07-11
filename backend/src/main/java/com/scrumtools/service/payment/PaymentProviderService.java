package com.scrumtools.service.payment;

import java.math.BigDecimal;

/**
 * Ödeme sağlayıcı soyutlaması.
 * IyzilinkPaymentProvider (iyzico.enabled=true) veya NoopPaymentProvider (fallback).
 * Superadmin manuel aktivasyon bu katmana hiç uğramaz — provider yapılandırılmadan da
 * satış (havale/EFT + manuel aktivasyon) mümkündür.
 */
public interface PaymentProviderService {

    /** Tek kullanımlık ödeme linki oluşturur. */
    PaymentLink createPaymentLink(CreateLinkCommand command);

    /** Link/ödeme durumunu sağlayıcıdan sorgular (webhook doğrulama + reconciliation). */
    LinkStatus getLinkStatus(String token);

    /** Linki devre dışı bırakır (süresi dolan PENDING işlemler için). */
    void disableLink(String token);

    boolean isEnabled();

    record CreateLinkCommand(
            String name,          // Ödeme sayfasında görünen ürün adı
            String description,
            BigDecimal price,     // KDV dahil, TRY
            String conversationId // PaymentTransaction.id — eşleştirme anahtarı
    ) {}

    record PaymentLink(String token, String url) {}

    record LinkStatus(boolean paid, String providerPaymentId, String rawResponse) {}
}
