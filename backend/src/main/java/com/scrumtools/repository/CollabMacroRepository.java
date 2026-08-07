package com.scrumtools.repository;

import com.scrumtools.entity.CollabMacro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
}
