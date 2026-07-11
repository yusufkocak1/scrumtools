package com.scrumtools.entity;

import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ödeme kaydı (iyzilink veya manuel).
 * conversationId = bu kaydın id'si (string) — iyzico tarafındaki işlemle
 * eşleştirme ve idempotency anahtarı olarak kullanılır.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    // KDV dahil brüt tutar
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "TRY";

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("20.00");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // IYZILINK | MANUAL
    @Column(nullable = false)
    private String provider;

    @Column(unique = true)
    private String conversationId;

    @Column(unique = true)
    private String iyzilinkToken;

    @Column(length = 1024)
    private String iyzilinkUrl;

    private String providerPaymentId;

    @Column(columnDefinition = "TEXT")
    private String rawWebhookPayload;

    private LocalDateTime paidAt;

    // Ödeme linkini başlatan kişi (müşteri veya superadmin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
