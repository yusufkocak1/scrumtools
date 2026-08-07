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

    /**
     * İzleme ekranı (§12): sıkıştırılmamış yükü en büyük dokümanlar.
     *
     * <p>Amaç "kaynak tükenmeden görmek": disk şişmesi tek bir uzun ömürlü
     * dokümandan gelir (R3) ve toplamda görünmez. Sorgu yalnızca yönetici
     * ekranından, isteğe bağlı çalışır — sıcak yolda değil.
     */
    /*
     * Takma adlar tırnak içinde: PostgreSQL tırnaksız tanımlayıcıları küçük
     * harfe katlar ({@code payloadBytes} → {@code payloadbytes}) ve arayüz
     * izdüşümünün getter eşlemesi bundan etkilenir.
     */
    @Query(value = """
            SELECT u.document_id AS "documentId",
                   d.title       AS "title",
                   COUNT(*)      AS "updateCount",
                   COALESCE(SUM(OCTET_LENGTH(u.payload)), 0) AS "payloadBytes"
            FROM collab_updates u
            JOIN collab_documents d ON d.id = u.document_id
            GROUP BY u.document_id, d.title
            ORDER BY "payloadBytes" DESC
            LIMIT 10
            """, nativeQuery = true)
    List<TopDocumentRow> findTopDocumentsByPayload();

    /** {@link #findTopDocumentsByPayload} satır izdüşümü. */
    interface TopDocumentRow {
        UUID getDocumentId();

        String getTitle();

        long getUpdateCount();

        long getPayloadBytes();
    }
}
