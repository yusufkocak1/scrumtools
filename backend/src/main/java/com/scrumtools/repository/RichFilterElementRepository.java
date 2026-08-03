package com.scrumtools.repository;

import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.RichFilterElementKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RichFilterElementRepository extends JpaRepository<RichFilterElement, UUID> {

    List<RichFilterElement> findByRichFilterIdOrderByPositionAsc(UUID richFilterId);

    /** Tür bazlı liste — akıllı filtre sırası sınıflandırmayı belirlediği için sıralı gelir. */
    List<RichFilterElement> findByRichFilterIdAndKindOrderByPositionAsc(UUID richFilterId,
                                                                       RichFilterElementKind kind);

    long countByRichFilterId(UUID richFilterId);
}
