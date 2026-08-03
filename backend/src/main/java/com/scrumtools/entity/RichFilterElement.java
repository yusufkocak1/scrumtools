package com.scrumtools.entity;

import com.scrumtools.entity.enums.RichFilterElementKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zengin filtrenin bir alt öğesi — akıllı filtre, dinamik filtre, görünüm, kuyruk…
 *
 * Tür başına ayrı tablo açmak yerine ortak sütunlar burada tipli durur, türe özel
 * alanlar {@link #config} (JSONB) içinde taşınır. Doğrulama uygulama katmanında
 * yapılır; böylece yeni bir öğe türü şema değişikliği gerektirmez.
 *
 * config şemaları (bkz. RICH_FILTER_PLAN.md §5):
 * <pre>
 *   SMART_FILTER   { "icon": "🔥" }                       — sorgu ve renk sütunlarda
 *   STATIC_FILTER  { "options": [{id,label,query}], "multi": false, "default": "o1" }
 *   DYNAMIC_FILTER { "field": "assignee", "multi": true, "maxOptions": 25 }
 *   VIEW           { "selection": {...}, "default": true }
 *   QUEUE          { "smartFilterIds": [...], "columns": [...], "limit": 10 }
 *   CUSTOM_VALUE   { "kind": "AGE" | "CYCLE_TIME" | "FIELD", "field": "storyPoints" }
 *   RATIO          { "numerator": {...}, "denominator": {...}, "target": 0.8 }
 *   TIME_SERIES    { "source": "smart", "smartFilterId": "…", "interval": "day" }
 * </pre>
 */
@Entity
@Table(name = "rich_filter_elements", indexes = {
        @Index(name = "idx_rf_element_filter", columnList = "rich_filter_id"),
        @Index(name = "idx_rf_element_kind", columnList = "kind")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RichFilterElement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rich_filter_id", nullable = false)
    private RichFilter richFilter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RichFilterElementKind kind;

    @Column(nullable = false)
    private String name;

    /** STQL parçası — akıllı filtrelerde zorunlu, görünüm/kuyruk gibi türlerde boş. */
    @Column(columnDefinition = "TEXT")
    private String query;

    /** #RRGGBB — grafiklerin ve görev etiketlerinin rengi. */
    @Column(length = 9)
    private String color;

    /**
     * Öğenin kendi türü içindeki sırası.
     *
     * Akıllı filtrelerde sıra sonucu değiştirir: bir görev, kendisine uyan
     * <b>ilk</b> akıllı filtrenin rengini ve etiketini alır.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** config okumaları için null'a karşı güvenli erişim. */
    public Map<String, Object> configOrEmpty() {
        return config == null ? new LinkedHashMap<>() : config;
    }
}
