package com.scrumtools.repository;

import com.scrumtools.entity.CollabDocument;
import com.scrumtools.entity.DocPage;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.entity.enums.CollabLinkMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollabDocumentRepository extends JpaRepository<CollabDocument, UUID> {

    /**
     * Liste ekranı: tip ve takım filtreleri isteğe bağlıdır ({@code null} geçilirse
     * uygulanmaz). Serbest metin filtresi için "filtre yok" karşılığı <b>boş dize</b>'dir.
     *
     * <p>Arama hem başlığa hem {@code snapshot_text}'e bakar — kullanıcı çoğu zaman
     * dokümanın adını değil içinde geçen bir kelimeyi hatırlar.
     *
     * <p><b>{@code query} asla {@code null} geçilmemeli</b> ve boşluk kontrolü neden
     * {@code IS NULL} değil {@code = ''}: Hibernate bir parametrenin tipini ya çalışma
     * anındaki değerin sınıfından ya da JPQL'deki kullanımından çıkarır. Bu parametrenin
     * diğer bütün kullanımları {@code LOWER(CONCAT(...))} fonksiyon çağrılarının içinde
     * ve çıkarım oraya inmiyor; {@code :query IS NULL} de tip taşımadığından, değer null
     * geldiğinde çıkarım tamamen başarısız oluyordu. O durumda Hibernate parametreyi
     * "serileştirilebilir nesne" varsayıp {@code VARBINARY} bağlıyor, PostgreSQL
     * {@code '%' || $n} ifadesini bytea birleştirmesi olarak çözüyor ve sorgu
     * <i>function lower(bytea) does not exist</i> ile patlıyordu — üstelik hata mesajı
     * {@code snapshot_text} kolonunu değil bu parametreyi işaret ediyor.
     * {@code = ''} biçimi iki yönden de kapatıyor: değer hep {@code String} oluyor ve
     * dize sabitiyle karşılaştırma tipi JPQL'den de çıkarılabilir kılıyor.
     * ({@code :type}/{@code :teamId} aynı {@code IS NULL} deseninde sorunsuz, çünkü
     * onların {@code d.type = :type} karşılaştırması Hibernate'e tipi veriyor.)
     */
    @Query("""
            SELECT d FROM CollabDocument d
            WHERE d.project.id = :projectId
              AND d.archivedAt IS NULL
              AND (:type IS NULL OR d.type = :type)
              AND (:teamId IS NULL OR d.team.id = :teamId)
              AND (:query = '' OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(d.snapshotText) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY d.updatedAt DESC
            """)
    List<CollabDocument> search(@Param("projectId") UUID projectId,
                                @Param("type") CollabDocumentType type,
                                @Param("teamId") UUID teamId,
                                @Param("query") String query);

    /** FREE paket doküman kotası için. */
    long countByProjectOrganizationIdAndArchivedAtIsNull(UUID organizationId);

    /** Faz 2 — bir Docs sayfasının ortak dokümanı (idempotent "Ortak Düzenle"). */
    Optional<CollabDocument> findByDocPageId(UUID docPageId);

    @Query("SELECT d.lastSeq FROM CollabDocument d WHERE d.id = :id")
    Long findLastSeq(@Param("id") UUID id);

    /**
     * Append ve anlık görüntü yolları neden <b>hedefli UPDATE</b> kullanıyor:
     * ikisi de aynı satıra dokunur ama farklı alanlara. Entity'yi yükleyip
     * {@code save()} etselerdi, biri diğerinin arada yazdığı alanı eski değeriyle
     * geri yazardı (klasik kayıp güncelleme). Alan bazlı UPDATE bu sınıfı tamamen
     * ortadan kaldırır.
     *
     * <p>Toplu güncelleme {@code @UpdateTimestamp}'i tetiklemez; {@code updatedAt}
     * bu yüzden açıkça geçilir.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CollabDocument d SET d.lastSeq = :lastSeq, d.updatedAt = :now WHERE d.id = :id")
    void bumpLastSeq(@Param("id") UUID id,
                     @Param("lastSeq") Long lastSeq,
                     @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CollabDocument d
            SET d.state = :state,
                d.stateVector = :stateVector,
                d.snapshotText = :snapshotText,
                d.snapshotSeq = :snapshotSeq,
                d.updatedBy = :updatedBy,
                d.updatedAt = :now
            WHERE d.id = :id
            """)
    void applySnapshot(@Param("id") UUID id,
                       @Param("state") byte[] state,
                       @Param("stateVector") byte[] stateVector,
                       @Param("snapshotText") String snapshotText,
                       @Param("snapshotSeq") Long snapshotSeq,
                       @Param("updatedBy") User updatedBy,
                       @Param("now") LocalDateTime now);

    /**
     * Üstveri güncellemesi. Entity {@code save()} edilmiyor: yüklenen kopya
     * {@code lastSeq}'i de taşır ve kullanıcı başlığı değiştirirken bir başkası
     * yazıyorsa, o alan eski değeriyle geri yazılırdı — sıra numaraları çakışır
     * ve tekillik kısıtı patlardı.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CollabDocument d
            SET d.title = :title,
                d.language = :language,
                d.team = :team,
                d.updatedBy = :updatedBy,
                d.updatedAt = :now
            WHERE d.id = :id
            """)
    void updateMetadata(@Param("id") UUID id,
                        @Param("title") String title,
                        @Param("language") String language,
                        @Param("team") Team team,
                        @Param("updatedBy") User updatedBy,
                        @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CollabDocument d
            SET d.archivedAt = :now, d.updatedBy = :updatedBy, d.updatedAt = :now
            WHERE d.id = :id
            """)
    void archive(@Param("id") UUID id,
                 @Param("updatedBy") User updatedBy,
                 @Param("now") LocalDateTime now);

    /**
     * Tohumlama hakkını atomik olarak talep eder (plan Y1/R2).
     *
     * <p>Koşul üç katmanlıdır: henüz kimse tohumlamamış ({@code seeded_at IS NULL}),
     * hiç güncelleme yazılmamış ({@code last_seq = 0}) ve anlık görüntü yok.
     * İki istemci sayfayı aynı anda açtığında yalnızca biri 1 döner; diğeri 0
     * alır ve normal senkron yoluna düşer. Tek başına "içerik boş mu" kontrolü
     * yeterli olmazdı — iki istemci arasında yarış penceresi kalırdı.
     *
     * @return güncellenen satır sayısı; 1 ise hak bu çağırana verildi
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CollabDocument d SET d.seededAt = :now
            WHERE d.id = :id
              AND d.seededAt IS NULL
              AND d.lastSeq = 0
              AND d.state IS NULL
            """)
    int claimSeed(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /**
     * Tohumlamayı koşulsuz olarak yapılmış işaretler.
     *
     * <p>Y2'de kullanılır: Docs'a kaydedilen bir doküman zaten kendi içeriğiyle
     * bir sayfa doğurmuştur, o sayfayı geri aktarmak içeriği ikilerdi.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CollabDocument d SET d.seededAt = :now WHERE d.id = :id AND d.seededAt IS NULL")
    void markSeeded(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /** Docs bağlantısı kurar/çözer (Y1/Y2). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE CollabDocument d
            SET d.docPage = :docPage, d.linkMode = :linkMode, d.updatedBy = :updatedBy, d.updatedAt = :now
            WHERE d.id = :id
            """)
    void linkDocPage(@Param("id") UUID id,
                     @Param("docPage") DocPage docPage,
                     @Param("linkMode") CollabLinkMode linkMode,
                     @Param("updatedBy") User updatedBy,
                     @Param("now") LocalDateTime now);

    /** Sıkıştırma görevi için: anlık görüntüden bu yana çok delta biriktirmiş dokümanlar. */
    @Query("""
            SELECT d.id FROM CollabDocument d
            WHERE d.archivedAt IS NULL
              AND d.lastSeq - d.snapshotSeq >= :threshold
            """)
    List<UUID> findNeedingCompaction(@Param("threshold") long threshold);
}
