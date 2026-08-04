package com.scrumtools.entity;

import com.scrumtools.entity.enums.SeriesPointSource;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bir zaman serisi öğesinin tek günlük değeri.
 *
 * Zaman serisi, sorgu motorunun canlı olarak cevaplayamayacağı tek soru: geçmişteki
 * satır artık yok. Bu yüzden — ve yalnız bu yüzden — türetilmiş veri saklanıyor
 * (bkz. RICH_FILTER_PLAN.md — K13, K20: rollup yok kararının bilinçli istisnası).
 *
 * Gün başına tek satır: {@code (element_id, bucket_date)} benzersizdir. Gecelik iş
 * aynı gün ikinci kez çalışırsa var olan satırı günceller, ikinci bir nokta yazmaz.
 */
@Entity
@Table(name = "rich_filter_series_points",
        uniqueConstraints = @UniqueConstraint(name = "uk_series_point_element_date",
                columnNames = {"element_id", "bucket_date"}),
        indexes = @Index(name = "idx_series_point_element_date", columnList = "element_id, bucket_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RichFilterSeriesPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Bağlı olduğu TIME_SERIES öğesi; öğe silinince noktaları da silinir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_id", nullable = false)
    private RichFilterElement element;

    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    /** Ölçülen değer — v1'de görev sayısı, ileride toplam da olabilir. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SeriesPointSource source = SeriesPointSource.SNAPSHOT;

    /** Noktanın hesaplandığı an — kurgulanmış noktalarda kurgunun yapıldığı gün. */
    @Column(name = "computed_at", nullable = false)
    @Builder.Default
    private LocalDateTime computedAt = LocalDateTime.now();
}
