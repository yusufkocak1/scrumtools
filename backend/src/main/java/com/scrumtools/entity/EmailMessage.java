package com.scrumtools.entity;

import com.scrumtools.entity.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gönderilen bir uygulama e-postasının durumu. Kayıt gönderim anında QUEUED olarak
 * açılır, PostForge webhook'ları (message.sent/failed/opened/clicked) üzerinden güncellenir.
 * <p>
 * Şablon parametreleri BİLEREK saklanmaz — davet/şifre sıfırlama linkleri tek kullanımlık
 * token içerir, DB'ye düşmemeleri gerekir.
 */
@Entity
@Table(name = "email_messages", indexes = {
        @Index(name = "idx_email_messages_recipient", columnList = "recipient"),
        @Index(name = "idx_email_messages_org_template", columnList = "organization_id, templateCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** PostForge'un döndürdüğü mesaj kimliği — webhook'larla eşleşme anahtarı. */
    @Column(unique = true, nullable = false, length = 100)
    private String messageId;

    @Column(nullable = false, length = 100)
    private String templateCode;

    @Column(nullable = false, length = 255)
    private String recipient;

    /**
     * Davet maillerinde doldurulur — davet listesi bu alanla organizasyona göre
     * filtrelenir. Diğer şablonlarda null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /** Davet maillerinde hedef kullanıcı; diğer şablonlarda null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime failedAt;

    /** message.failed olayındaki hata açıklaması (varsa). */
    @Column(length = 500)
    private String failureReason;

    /** İlk açılma. Açılma 1x1 pikselle ölçüldüğü için her zaman eksik sayılır. */
    private LocalDateTime openedAt;

    /** İlk tıklama. Tıklanan mail PostForge tarafında açılmış da sayılır. */
    private LocalDateTime firstClickedAt;

    @Builder.Default
    @Column(nullable = false)
    private int openCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int clickCount = 0;

    /** En son işlenen webhook olayının zamanı (teşhis için). */
    private LocalDateTime lastEventAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
