package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Denormalize görüntü cache'i (plan code) — source of truth: Subscription → Plan.
    // SubscriptionService her durum değişiminde günceller.
    @Column(nullable = false)
    @Builder.Default
    private String plan = "FREE";

    // Org bazlı üye limiti override'ı (özel anlaşmalar için) — null = plan limiti geçerli
    @Column
    private Integer maxMembers;

    // Superadmin tarafından askıya alınmış org — üyelik kontrollerinde erişim engellenir
    @Column
    private Boolean suspended;

    /** Null-safe askıya alınma kontrolü (null = askıda değil). */
    public boolean isSuspendedOrg() {
        return Boolean.TRUE.equals(suspended);
    }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

