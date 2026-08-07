package com.scrumtools.repository;

import com.scrumtools.entity.CollabUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CollabUpdateRepository extends JpaRepository<CollabUpdate, Long> {

    /** Açılışta yeniden oynatma sırası — anlık görüntüden sonraki deltalar. */
    List<CollabUpdate> findByDocumentIdAndSeqGreaterThanOrderBySeqAsc(UUID documentId, Long seq);

    /**
     * Sıkıştırma: anlık görüntünün kapsadığı satırlar artık gereksizdir.
     * {@code <=} sınırı önemli — {@code <} kullanılsaydı her sıkıştırmada bir satır
     * sonsuza kadar kalırdı.
     */
    @Modifying
    @Query("DELETE FROM CollabUpdate u WHERE u.document.id = :documentId AND u.seq <= :seq")
    int deleteUpToSeq(@Param("documentId") UUID documentId, @Param("seq") Long seq);

    long countByDocumentId(UUID documentId);

    @Query("SELECT COALESCE(SUM(LENGTH(u.payload)), 0) FROM CollabUpdate u WHERE u.document.id = :documentId")
    long sumPayloadBytes(@Param("documentId") UUID documentId);
}
