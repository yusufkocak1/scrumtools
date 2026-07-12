package com.scrumtools.entity;

import com.scrumtools.entity.enums.ErrorSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tekil hata olayı — kullanıcıya gösterilen takip numarasını (trackingCode) taşır,
 * her olay bir ErrorGroup'a bağlıdır.
 */
@Entity
@Table(name = "error_events", indexes = {
        @Index(name = "idx_error_event_tracking_code", columnList = "tracking_code", unique = true),
        @Index(name = "idx_error_event_group", columnList = "group_id"),
        @Index(name = "idx_error_event_user", columnList = "user_email"),
        @Index(name = "idx_error_event_occurred", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Kullanıcıya gösterilen takip numarası, örn. ERR-7K3M2A9X */
    @Column(name = "tracking_code", nullable = false, length = 16, unique = true)
    private String trackingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ErrorGroup group;

    /** Hatayı yaşayan kullanıcı (anonim/istem dışı durumlarda null) */
    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ErrorSource source;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    /** Backend'de istek path'i, frontend'de sayfa URL'i */
    @Column(length = 1024)
    private String endpoint;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 512)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;
}
