package com.scrumtools.entity;

import com.scrumtools.entity.enums.SupportCategory;
import com.scrumtools.entity.enums.SupportTicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Destek talebi — kullanıcı bazlıdır; organizasyon bilgisi oluşturma anında
 * snapshot olarak saklanır (FK değil), sadece admin panelinde filtreleme içindir.
 */
@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_support_ticket_user", columnList = "user_id"),
        @Index(name = "idx_support_ticket_status", columnList = "status"),
        @Index(name = "idx_support_ticket_org", columnList = "organization_id"),
        @Index(name = "idx_support_ticket_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    /** Oluşturma anındaki organizasyon snapshot'ı (admin filtresi için) */
    @Column(name = "organization_id")
    private UUID organizationId;

    private String organizationName;

    /** İlişkili hata takip numarası (varsa) — ErrorEvent.trackingCode ile gevşek bağ */
    @Column(length = 16)
    private String errorTrackingCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
