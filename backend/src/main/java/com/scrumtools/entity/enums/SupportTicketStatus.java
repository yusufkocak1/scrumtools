package com.scrumtools.entity.enums;

public enum SupportTicketStatus {
    OPEN,          // Açık — destek ekibinin aksiyonu bekleniyor
    IN_PROGRESS,   // İnceleniyor
    WAITING_USER,  // Kullanıcıdan yanıt bekleniyor
    RESOLVED,      // Çözüldü
    CLOSED         // Kapatıldı — yeni mesaj eklenemez
}
