package com.scrumtools.repository;

import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.RichFilterElementKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RichFilterElementRepository extends JpaRepository<RichFilterElement, UUID> {

    List<RichFilterElement> findByRichFilterIdOrderByPositionAsc(UUID richFilterId);

    /**
     * Tüm takımlardaki belirli türde öğeler — gecelik zaman serisi işi için.
     * Zengin filtre birlikte çekilir: iş, her öğe için ayrı sorgu atmasın.
     */
    @Query("SELECT e FROM RichFilterElement e JOIN FETCH e.richFilter WHERE e.kind = :kind")
    List<RichFilterElement> findByKindWithFilter(@Param("kind") RichFilterElementKind kind);

    /** Tür bazlı liste — akıllı filtre sırası sınıflandırmayı belirlediği için sıralı gelir. */
    List<RichFilterElement> findByRichFilterIdAndKindOrderByPositionAsc(UUID richFilterId,
                                                                       RichFilterElementKind kind);

    long countByRichFilterId(UUID richFilterId);
}
