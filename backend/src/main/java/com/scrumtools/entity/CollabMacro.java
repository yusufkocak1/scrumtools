package com.scrumtools.entity;

import com.scrumtools.entity.enums.MacroTriggerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doküman ya da proje kütüphanesi makrosu (COLLAB_WORKSPACE_PLAN.md §5 / §9).
 *
 * <p>Makro <b>çalıştıranın</b> yetkisiyle koşar (§9.2). Bu, kullanışlı olduğu
 * kadar tehlikelidir: yetkisi düşük biri makro yazıp yetkisi yüksek birine
 * çalıştırtabilir. Modeldeki tek savunma {@link #sourceHash} + {@link #approvedAt}
 * çiftidir — kaynak değişince onay kendiliğinden düşer.
 */
@Entity
@Table(name = "collab_macros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabMacro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** {@code null} ise proje kütüphanesi makrosu — her dokümanda kullanılabilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private CollabDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    @Builder.Default
    private MacroTriggerType triggerType = MacroTriggerType.MANUAL;

    /** Faz 5 (sunucu tarafı yürütme) için; Faz 4'te yalnızca saklanır. */
    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Onaylanan kaynağın SHA-256'sı.
     *
     * <p>Onayı kaynağa değil <b>kaynağın özetine</b> bağlamak şart: aksi hâlde
     * onaydan sonra betiği değiştirmek, incelenmemiş kodu onaylı göstermenin en
     * kolay yolu olurdu.
     */
    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    /**
     * Statik taramada bulunan API alanları (virgülle ayrık, örn. {@code tasks,docs}).
     *
     * <p>İlk çalıştırmadaki rıza diyaloğu bunu gösterir (§9.2). Sunucuda
     * saklanıyor ki diyalog, istemcinin kendi taramasına güvenmek zorunda kalmasın.
     */
    @Column(name = "api_scopes", length = 500)
    private String apiScopes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Onay, yalnızca onaylanan kaynak hâlâ yürürlükteyse geçerlidir. */
    public boolean isApproved(String currentHash) {
        return approvedAt != null && sourceHash != null && sourceHash.equals(currentHash);
    }
}
