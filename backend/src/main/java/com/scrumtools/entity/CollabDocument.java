package com.scrumtools.entity;

import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.entity.enums.CollabLinkMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Eş zamanlı düzenlenen bir ortak çalışma dokümanı (COLLAB_WORKSPACE_PLAN.md §5).
 *
 * <p>İçerik iki biçimde durur:
 * <ul>
 *   <li>{@link #state} — sıkıştırılmış Yjs durumu. Doğruluk kaynağı budur; sunucu
 *       <b>açmaz, yorumlamaz</b> (plan K2). Yanına {@code collab_updates} tablosundaki
 *       henüz sıkıştırılmamış deltalar eklenir.</li>
 *   <li>{@link #snapshotText} — insanın okuyabildiği çıktı (HTML / kod / JSON).
 *       Yalnızca <b>türev</b>dir: liste ekranı, arama ve Docs'a yazma için üretilir,
 *       düzenleme buradan yüklenmez.</li>
 * </ul>
 *
 * <p>Eski Code Share'in aksine {@code tag} yoktur (plan D1): dokümanlar UUID ile
 * adreslenir, böylece "aynı etiketi iki kişi farklı amaçla kullandı" çakışması
 * ortadan kalkar.
 */
@Entity
@Table(name = "collab_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Paket denetimi (COLLAB_WORKSPACE / COLLAB_SHEET) bu organizasyon üzerinden yapılır. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /** Plan D4 — proje birincil kapsam; Docs ile aynı hizada yaşar. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** İsteğe bağlı etiket/filtre; yetkiye etkisi yoktur (plan D4). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollabDocumentType type;

    @Column(nullable = false, length = 500)
    private String title;

    /** Yalnızca CODE için anlamlı: Monaco dil kimliği (javascript, java...). */
    @Column(length = 50)
    private String language;

    /** Faz 2 — Docs aynalaması. Bir sayfanın en fazla bir ortak dokümanı olabilir. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_page_id", unique = true)
    private DocPage docPage;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_mode", nullable = false, length = 20)
    @Builder.Default
    private CollabLinkMode linkMode = CollabLinkMode.NONE;

    /** Sıkıştırılmış Yjs durumu (Y.encodeStateAsUpdate). */
    @Column(name = "state", columnDefinition = "bytea")
    private byte[] state;

    /** Yjs durum vektörü — istemcinin neyi eksik olduğunu hesaplaması için. */
    @Column(name = "state_vector", columnDefinition = "bytea")
    private byte[] stateVector;

    /** Okunabilir çıktı: TEXT → sanitize edilmiş HTML, CODE → düz metin, SHEET → JSON. */
    @Column(name = "snapshot_text", columnDefinition = "TEXT")
    private String snapshotText;

    /**
     * {@link #state} hangi {@code seq}'e kadar olan güncellemeleri içeriyor.
     * Sıkıştırma bu değere kadar olan satırları siler.
     */
    @Column(name = "snapshot_seq", nullable = false)
    @Builder.Default
    private Long snapshotSeq = 0L;

    /** Yazılan son güncellemenin sırası. Yeni güncellemeler buradan devam eder. */
    @Column(name = "last_seq", nullable = false)
    @Builder.Default
    private Long lastSeq = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    /**
     * Docs sayfasından tohumlama (seeding) kilidi — plan Y1/R2.
     *
     * <p>Bir {@code DocPage} ortak düzenlemeye açıldığında mevcut HTML içeriğinin
     * Y.Doc'a aktarılması gerekir. Bunu sunucu yapamaz (orada Yjs yok, K2), yani
     * bir istemci yapar. İki istemci sayfayı aynı anda açarsa <b>ikisi de</b>
     * aktarır ve içerik ikilenir. Bu alan, tohumlama hakkının atomik olarak tek
     * bir istemciye verilmesini sağlar: koşullu {@code UPDATE ... WHERE
     * seeded_at IS NULL} yalnızca bir kez başarılı olur.
     */
    @Column(name = "seeded_at")
    private LocalDateTime seededAt;

    /** Yumuşak silme — dolu ise doküman listelerde görünmez ve açılamaz. */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
