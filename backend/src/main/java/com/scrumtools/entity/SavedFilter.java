package com.scrumtools.entity;

import com.scrumtools.entity.enums.FilterVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Kaydedilmiş STQL sorgusu — Jira'daki "saved filter" karşılığı.
 *
 * Sorgu metni olduğu gibi saklanır (çözümlenmiş hâli değil): dil zamanla
 * genişlerken eski filtreler yeni motorla yeniden yorumlanabilsin diye.
 *
 * Dashboard widget'ları ileride bu kayıtlara id ile bağlanacak — widget kendi
 * sorgusunu taşımaz, kayıtlı filtreyi referans alır (bkz. DASHBOARD_WIDGET_ROADMAP.md).
 */
@Entity
@Table(name = "saved_filters", indexes = {
        @Index(name = "idx_saved_filter_team", columnList = "team_id"),
        @Index(name = "idx_saved_filter_owner", columnList = "owner_id"),
        @Index(name = "idx_saved_filter_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Filtreyi oluşturan kullanıcı — yalnız o düzenleyebilir/silebilir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * Filtrenin bağlı olduğu proje. PROJECT görünürlüğünde zorunludur;
     * diğer durumlarda filtre çalıştırılırken varsayılan proje kapsamı olarak kullanılır.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** STQL metni — çalıştırılırken çözümlenir. */
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FilterVisibility visibility = FilterVisibility.PRIVATE;

    /** Filtreyi yıldızlayan kullanıcıların id'leri — favorilerde üstte listelenir. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "saved_filter_favorites", joinColumns = @JoinColumn(name = "filter_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> favoritedBy = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
