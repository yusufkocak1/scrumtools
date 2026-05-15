package com.scrumtools.repository;

import com.scrumtools.entity.DocPermission;
import com.scrumtools.entity.enums.DocTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocPermissionRepository extends JpaRepository<DocPermission, UUID> {

    List<DocPermission> findBySpaceId(UUID spaceId);

    List<DocPermission> findByPageId(UUID pageId);

    @Query("""
        SELECT p FROM DocPermission p
        WHERE p.space.id = :spaceId
          AND p.targetType = :targetType
          AND p.targetId = :targetId
    """)
    List<DocPermission> findBySpaceAndTarget(UUID spaceId, DocTargetType targetType, UUID targetId);

    @Query("""
        SELECT p FROM DocPermission p
        WHERE p.page.id = :pageId
          AND p.targetType = :targetType
          AND p.targetId = :targetId
    """)
    List<DocPermission> findByPageAndTarget(UUID pageId, DocTargetType targetType, UUID targetId);

    @Query("""
        SELECT p FROM DocPermission p
        WHERE (p.space.id = :spaceId OR p.page.id IN (SELECT dp.id FROM DocPage dp WHERE dp.space.id = :spaceId))
          AND p.targetType = :targetType
          AND p.targetId = :targetId
    """)
    List<DocPermission> findAllBySpaceForTarget(UUID spaceId, DocTargetType targetType, UUID targetId);

    void deleteBySpaceId(UUID spaceId);

    void deleteByPageId(UUID pageId);
}

