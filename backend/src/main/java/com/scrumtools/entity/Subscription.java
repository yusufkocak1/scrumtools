package com.scrumtools.entity;

import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organizasyonun paket aboneliği.
 * Org başına en fazla bir canlı (TRIAL/ACTIVE/PAST_DUE) kayıt olabilir —
 * SubscriptionService katmanında enforce edilir. Terminal durumdaki
 * (EXPIRED/CANCELED) satırlar geçmiş olarak saklanır.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    // TRIAL aboneliklerde null
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    private LocalDateTime currentPeriodStart;

    private LocalDateTime currentPeriodEnd;

    private LocalDateTime trialEndsAt;

    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionSource source;

    // MANUAL aktivasyonda işlemi yapan superadmin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activated_by")
    private User activatedBy;

    // 7/3/1 gün hatırlatmalarının tekrar gönderilmemesi için bitmask (bit0=7g, bit1=3g, bit2=1g)
    @Column(nullable = false)
    @Builder.Default
    private Integer remindersSent = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
