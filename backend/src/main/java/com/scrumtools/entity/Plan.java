package com.scrumtools.entity;

import com.scrumtools.entity.enums.PlanFeature;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Satılabilir paket tanımı (FREE / PRO / MAX).
 * DB-driven: fiyat, limit ve özellikler superadmin panelinden düzenlenebilir.
 * Organization.plan alanı bu entity'nin code değerini denormalize cache olarak tutar;
 * source of truth Subscription → Plan ilişkisidir.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // null = sınırsız
    @Column
    private Integer maxMembers;

    // null = sınırsız
    @Column
    private Integer maxProjects;

    @ElementCollection(targetClass = PlanFeature.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "feature")
    @Builder.Default
    private Set<PlanFeature> features = new HashSet<>();

    // KDV dahil brüt fiyatlar (iyzilink müşterinin ödediği tutarı ister)
    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyPriceTry;

    @Column(precision = 10, scale = 2)
    private BigDecimal yearlyPriceTry;

    // Sadece trial'ı temsil eden üst pakette anlamlı
    @Column
    private Integer trialDays;

    // Canlı aboneliği olmayan org'ların düştüğü paket (FREE)
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    // Müşteri arayüzündeki plan kartlarında görünür mü
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
