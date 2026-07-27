package com.scrumtools.entity.enums;

public enum EmailStatus {
    QUEUED,  // PostForge isteği kabul etti, henüz teslim bildirimi gelmedi
    SENT,    // message.sent — gönderici SMTP sunucusuna teslim edildi
    FAILED   // message.failed — tüm yeniden denemeler tükendi
}
