package com.scrumtools.entity;

import com.scrumtools.entity.enums.ErrorGroupStatus;
import com.scrumtools.entity.enums.ErrorSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hata grubu — aynı fingerprint'e sahip hata olayları tek grupta toplanır.
 * Fingerprint: hata tipi + oluştuğu yer üzerinden SHA-256 (bkz. ErrorTrackingService).
 */
@Entity
@Table(name = "error_groups", indexes = {
        @Index(name = "idx_error_group_fingerprint", columnList = "fingerprint", unique = true),
        @Index(name = "idx_error_group_status", columnList = "status"),
        @Index(name = "idx_error_group_last_seen", columnList = "last_seen_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ErrorSource source;

    /** Root cause exception sınıfının FQN'i (frontend için errorName) */
    @Column(nullable = false, length = 512)
    private String exceptionType;

    /** Hatanın oluştuğu yer: backend'de "className.methodName", frontend'de tepe stack frame */
    @Column(length = 512)
    private String location;

    /** Gruptaki ilk olayın hata mesajı (örnek olarak saklanır) */
    @Column(columnDefinition = "TEXT")
    private String sampleMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ErrorGroupStatus status = ErrorGroupStatus.NEW;

    @Column(nullable = false)
    @Builder.Default
    private Long occurrenceCount = 0L;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    private LocalDateTime resolvedAt;

    /** Çözüldü işaretleyen admin email'i */
    private String resolvedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
