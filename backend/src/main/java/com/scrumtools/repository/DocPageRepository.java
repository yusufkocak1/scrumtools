package com.scrumtools.repository;

import com.scrumtools.entity.DocPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocPageRepository extends JpaRepository<DocPage, UUID> {

    List<DocPage> findBySpaceIdOrderBySortOrderAsc(UUID spaceId);

    List<DocPage> findBySpaceIdAndParentPageIsNullOrderBySortOrderAsc(UUID spaceId);

    List<DocPage> findByParentPageIdOrderBySortOrderAsc(UUID parentPageId);

    Optional<DocPage> findBySpaceIdAndSlug(UUID spaceId, String slug);

    boolean existsBySpaceIdAndSlug(UUID spaceId, String slug);

    int countBySpaceId(UUID spaceId);
}

