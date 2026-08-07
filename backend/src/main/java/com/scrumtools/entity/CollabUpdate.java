package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tek bir CRDT güncelleme paketi — append-only kayıt (COLLAB_WORKSPACE_PLAN.md §5).
 *
 * <p><b>Veri kaybına karşı asıl güvence budur, anlık görüntü değil.</b> "Kimse
 * kaydetmeden herkes çıktı" durumunda bile bir sonraki açılışta bu satırlar
 * yeniden oynatılır. Anlık görüntü yalnızca oynatılacak satır sayısını azaltır.
 *
 * <p>Satırlar sıkıştırma görevinde ({@code seq <= snapshot_seq}) silinir; yoksa
 * uzun ömürlü bir doküman diski büyütür (plan R3).
 */
@Entity
@Table(
        name = "collab_updates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collab_update_doc_seq",
                columnNames = {"document_id", "seq"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabUpdate {

    /**
     * IDENTITY değil SEQUENCE: Hibernate, identity kolonlarda her satır için
     * üretilen anahtarı geri okumak zorunda olduğundan JDBC toplu eklemeyi
     * kapatır. Gruplamanın (§12) tek amacı yazma sayısını düşürmek olduğu için
     * burada toplu ekleme şart.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "collab_update_seq")
    @SequenceGenerator(name = "collab_update_seq", sequenceName = "collab_updates_id_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private CollabDocument document;

    /** Doküman içinde artan sıra. Yeniden oynatma sırasını belirler. */
    @Column(name = "seq", nullable = false)
    private Long seq;

    /**
     * Ham Yjs güncellemesi. Birden fazla istemci paketi burada birleşik olabilir
     * (§12 append gruplama) — Yjs güncellemeleri birbirine eklenebilir olduğu için
     * bu güvenlidir.
     */
    @Column(name = "payload", columnDefinition = "bytea", nullable = false)
    private byte[] payload;

    /** Denetim için; kullanıcı silinmiş olabilir, o yüzden nullable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
