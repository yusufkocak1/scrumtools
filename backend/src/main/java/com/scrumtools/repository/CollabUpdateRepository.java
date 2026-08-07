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

    /**
     * Sıkıştırma eşiği için toplam ham yük boyutu (§5).
     *
     * <p>Native sorgu, çünkü HQL'in {@code LENGTH()}'i {@code character_length()}
     * olarak çözülüyor ve Hibernate 6 bunu {@code bytea} üzerinde reddediyor —
     * uygulama açılışta sorgu doğrulamasında patlıyordu. {@code bytea} için
     * doğru karşılık PostgreSQL'in {@code octet_length()}'i.
     */
    @Query(value = """
            SELECT COALESCE(SUM(OCTET_LENGTH(payload)), 0)
            FROM collab_updates WHERE document_id = :documentId
            """, nativeQuery = true)
    long sumPayloadBytes(@Param("documentId") UUID documentId);
}
