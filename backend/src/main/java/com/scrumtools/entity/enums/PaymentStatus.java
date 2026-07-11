package com.scrumtools.entity.enums;

public enum PaymentStatus {
    PENDING,   // Link oluşturuldu, ödeme bekleniyor
    PAID,      // Ödeme onaylandı, abonelik aktive edildi
    FAILED,    // Ödeme başarısız
    EXPIRED,   // Link süresi doldu / kullanılmadı
    REFUNDED   // İade edildi
}
