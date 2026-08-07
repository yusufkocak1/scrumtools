package com.scrumtools.repository;

import com.scrumtools.entity.CollabMacro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollabMacroRepository extends JpaRepository<CollabMacro, UUID> {

    /**
     * Bir dokümanda kullanılabilir makrolar: dokümana özel olanlar + proje
     * kütüphanesi. Kütüphane makrolarının da gelmesi bilinçli — aynı betiği her
     * dokümana kopyalamak, onay akışını da kopyalamak demekti.
     */
    @Query("""
            SELECT m FROM CollabMacro m
            WHERE m.project.id = :projectId
              AND (m.document.id = :documentId OR m.document IS NULL)
            ORDER BY m.name ASC
            """)
    List<CollabMacro> findForDocument(@Param("projectId") UUID projectId,
                                      @Param("documentId") UUID documentId);

    List<CollabMacro> findByProjectIdAndDocumentIsNullOrderByNameAsc(UUID projectId);

    /**
     * Sunucu tarafı yürütme için makro + gerekli ilişkiler tek sorguda.
     *
     * <p>Fetch join şart: yürütme kuyruk iş parçacığında ve açık bir işlem
     * dışında koşuyor. Tembel bırakılan {@code project} ya da {@code approvedBy}
     * orada okunmaya çalışılınca {@code LazyInitializationException} verirdi —
     * üstelik hata yürütmenin ortasında, makronun yarısı çalışmışken gelirdi.
     */
    @Query("""
            SELECT m FROM CollabMacro m
            JOIN FETCH m.project p
            LEFT JOIN FETCH p.organization
            LEFT JOIN FETCH m.approvedBy
            LEFT JOIN FETCH m.createdBy
            WHERE m.id = :id
            """)
    Optional<CollabMacro> findByIdForExecution(@Param("id") UUID id);
}
