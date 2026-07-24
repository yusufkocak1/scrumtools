package com.scrumtools.entity.enums;

public enum NotificationType {
    // Task ile ilgili
    TASK_ASSIGNED,          // Task sana atandı
    TASK_UNASSIGNED,        // Task ataması kaldırıldı
    TASK_STATUS_CHANGED,    // Task durumu değişti
    TASK_PRIORITY_CHANGED,  // Task önceliği değişti
    TASK_COMMENTED,         // Task'a yorum yapıldı
    TASK_DUE_SOON,          // Bitiş tarihi yaklaşıyor
    TASK_OVERDUE,           // Bitiş tarihi geçti

    // Watcher ile ilgili
    WATCHED_TASK_UPDATED,   // İzlediğin task güncellendi

    // Davet ile ilgili
    INVITATION_RECEIVED,    // Takıma/projeye davet edildin
    INVITATION_ACCEPTED,    // Davetini kabul etti

    // Destek talepleri ile ilgili
    SUPPORT_TICKET_CREATED,        // Yeni destek talebi açıldı (adminlere)
    SUPPORT_TICKET_REPLIED,        // Destek talebine yanıt geldi
    SUPPORT_TICKET_STATUS_CHANGED, // Destek talebi durumu değişti

    // CI/CD ile ilgili
    CI_BUILD_SUCCEEDED,     // Tetiklenen build başarıyla tamamlandı
    CI_BUILD_FAILED,        // Tetiklenen build başarısız/kararsız bitti

    // Genel
    MENTION,                // @mention ile bahsedildin
    SYSTEM                  // Sistem bildirimi
}

