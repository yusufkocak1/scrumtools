package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Destek talebi mesajı — kullanıcı ile destek ekibi arasındaki yazışma.
 */
@Entity
@Table(name = "support_ticket_messages", indexes = {
        @Index(name = "idx_support_message_ticket", columnList = "ticket_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(nullable = false)
    private String authorEmail;

    private String authorName;

    /** Destek ekibi (admin) yanıtı mı? Frontend'de "Destek Ekibi" etiketiyle gösterilir. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdminReply = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
