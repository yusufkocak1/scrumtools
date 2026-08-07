package com.scrumtools.repository;

import com.scrumtools.entity.CollabSnapshot;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollabSnapshotRepository extends JpaRepository<CollabSnapshot, UUID> {

    List<CollabSnapshot> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Limit limit);

    Optional<CollabSnapshot> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    Optional<CollabSnapshot> findByIdAndDocumentId(UUID id, UUID documentId);

    /**
     * Saklama sınırı: dokümanın en yeni {@code keep} kaydı dışındakileri siler.
     *
     * <p>Anlık görüntü dakikada birkaç kez üretilebilir; sınırsız geçmiş, dar
     * sunucuda (plan D3) diski dolduran ikinci kalem olurdu — ilki ham güncelleme
     * log'u ve o zaten sıkıştırılıyor.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM collab_snapshots
            WHERE document_id = :documentId
              AND id NOT IN (
                  SELECT id FROM collab_snapshots
                  WHERE document_id = :documentId
                  ORDER BY created_at DESC
                  LIMIT :keep
              )
            """, nativeQuery = true)
    int pruneOlderThanNewest(@Param("documentId") UUID documentId, @Param("keep") int keep);
}
