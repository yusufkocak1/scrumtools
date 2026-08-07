package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ortak çalışma dokümanının zaman çizelgesindeki bir durağı
 * (COLLAB_WORKSPACE_PLAN.md §6 — {@code /history}).
 *
 * <p><b>Neden yalnızca okunabilir metin saklanıyor, ikili CRDT durumu değil:</b>
 * anlık görüntü dakikada birkaç kez üretilir; her birinin yanına sıkıştırılmış
 * Y.Doc durumunu koymak, dar sunucunun (plan D3) diskini geçmiş uğruna doldurur.
 * Metin hem çok küçüktür hem de geri yükleme için yeterlidir.
 *
 * <p><b>Geri yükleme neden "geri sarma" değil:</b> CRDT'de bir dokümanı eski
 * duruma <i>döndürmek</i> diye bir işlem yoktur — o, o sırada bağlı olan diğer
 * kullanıcıların durumuyla çelişirdi. Geri yükleme, eski metni yeni bir
 * düzenleme olarak uygulamaktır; geçmiş böylece hep ileri akar.
 */
@Entity
@Table(name = "collab_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private CollabDocument document;

    /** Anlık görüntünün kapsadığı son sıra — hangi noktaya karşılık geldiği. */
    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "snapshot_text", columnDefinition = "TEXT")
    private String snapshotText;

    /** Kaç kişi düzenliyordu — "Ortak düzenleme — 3 katılımcı" özetini üretir. */
    @Column(name = "participant_count")
    private Integer participantCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
