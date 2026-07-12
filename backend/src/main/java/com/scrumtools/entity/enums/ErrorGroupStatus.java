package com.scrumtools.entity.enums;

public enum ErrorGroupStatus {
    NEW,       // Yeni tespit edildi
    RESOLVED,  // Çözüldü olarak işaretlendi
    REOPENED,  // Çözüldükten sonra tekrar oluştu
    IGNORED    // Yoksayıldı (tekrarlar sayılır ama durum değişmez)
}
