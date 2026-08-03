package com.scrumtools.entity;

import com.scrumtools.entity.enums.FilterVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Zengin filtre — bir temel sorgu ve onun üstüne kurulmuş adlandırılmış parçaların kabı.
 *
 * Jira'daki "rich filter" karşılığı: dashboard widget'ları tek tek kendi sorgularını
 * taşımak yerine buna bağlanır, böylece bir sayfadaki bütün grafikler aynı soruyu
 * aynı sınıflandırmayla cevaplar.
 *
 * Temel sorgu iki biçimde verilebilir:
 *  - {@link #baseFilter} — var olan bir kayıtlı filtreye referans (tercih edilen);
 *    filtre güncellenince zengin filtre de kendiliğinden güncellenir.
 *  - {@link #baseQuery} — doğrudan STQL metni; kayıtlı filtre açmaya değmeyen durumlar için.
 *
 * Bkz. RICH_FILTER_PLAN.md
 */
@Entity
@Table(name = "rich_filters",
        uniqueConstraints = @UniqueConstraint(name = "uk_rich_filter_team_name",
                columnNames = {"team_id", "name"}),
        indexes = {
                @Index(name = "idx_rich_filter_team", columnList = "team_id"),
                @Index(name = "idx_rich_filter_owner", columnList = "owner_id"),
                @Index(name = "idx_rich_filter_project", columnList = "project_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RichFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Oluşturan kullanıcı — yalnız o düzenleyebilir/silebilir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** PROJECT görünürlüğünde zorunlu; diğer hâllerde varsayılan proje kapsamı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Takım içinde benzersizdir: STQL'deki {@code smart["ad"]} yazımı zengin filtreyi
     * adıyla çözer (bkz. RICH_FILTER_PLAN.md K18), aynı adlı iki kayıt sorguyu
     * belirsiz kılardı.
     */
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Temel sorgu — kayıtlı filtre referansı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_filter_id")
    private SavedFilter baseFilter;

    /** Temel sorgu — doğrudan STQL. {@link #baseFilter} doluysa yok sayılır. */
    @Column(name = "base_query", columnDefinition = "TEXT")
    private String baseQuery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FilterVisibility visibility = FilterVisibility.PRIVATE;

    /**
     * Alt öğeler — sıra anlamlıdır: akıllı filtrelerde "ilk eşleşen kazanır"
     * kuralını bu sıra belirler (bkz. RICH_FILTER_PLAN.md K4).
     */
    @OneToMany(mappedBy = "richFilter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<RichFilterElement> elements = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Temel sorgunun STQL metni — kayıtlı filtre varsa onunki geçerlidir. */
    public String effectiveBaseQuery() {
        if (baseFilter != null) return baseFilter.getQuery();
        return baseQuery == null ? "" : baseQuery;
    }

    /** Kapsam projesi: kendi projesi yoksa temel filtrenin projesi. */
    public Project effectiveProject() {
        if (project != null) return project;
        return baseFilter != null ? baseFilter.getProject() : null;
    }
}
