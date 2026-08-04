package com.scrumtools.entity;

import com.scrumtools.entity.enums.DashboardVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adlandırılmış pano — widget düzeninin kabı.
 *
 * Önceki model ({@code user_dashboards}) kullanıcı başına TEK satırdı: bir kişi
 * "sürüm hazırlığı" ile "günlük takip" panolarını aynı anda tutamıyor, birini
 * kurmak için diğerini bozmak zorunda kalıyordu. Artık pano bir takım altında
 * çoğaltılabilir ve istenirse takıma açılabilir.
 *
 * Takım bağı zorunlu: widget'ların hemen hepsi takım kapsamlı veri gösterir ve
 * panonun listelendiği yer aktif takımdır. Takımsız bir pano, açıldığı takıma
 * göre içeriği değişen ama adı sabit kalan bir kabuk olurdu.
 */
@Entity
@Table(name = "dashboards",
        uniqueConstraints = @UniqueConstraint(name = "uk_dashboard_owner_team_name",
                columnNames = {"owner_id", "team_id", "name"}),
        indexes = {
                @Index(name = "idx_dashboard_team", columnList = "team_id"),
                @Index(name = "idx_dashboard_owner", columnList = "owner_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Oluşturan kullanıcı — düzenleme hakkının birincil sahibi. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    /**
     * Widget listesi ve yerleşimi (JSONB). Sunucu içeriği yorumlamaz; şema
     * arayüzün elindedir ve widget tipleri arttıkça sunucuya dokunmadan büyür.
     * <p>
     * Örnek öğe:
     * {@code { "id":"RF_CHART-1712…", "type":"RF_CHART", "teamId":"…",
     *          "richFilterId":"…", "w":6, "h":320, "title":"…", "groupBy":"" }}
     * <p>
     * {@code w} 12'lik ızgarada kaç sütun kaplayacağı, {@code h} piksel
     * yüksekliğidir; {@code h} yoksa widget kendi doğal boyunda kalır.
     * Sıra dizinin kendi sırasıdır — ayrı bir pozisyon alanı tutmak, sürükleyip
     * bırakmada iki kaynağın (dizi sırası + alan) ayrışma riskini getirirdi.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> layout = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DashboardVisibility visibility = DashboardVisibility.PRIVATE;

    /** Sekme sırası — kullanıcı panolarını kendi çalışma düzenine göre dizer. */
    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
