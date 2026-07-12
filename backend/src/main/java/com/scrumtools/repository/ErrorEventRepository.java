package com.scrumtools.repository;

import com.scrumtools.entity.ErrorEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ErrorEventRepository extends JpaRepository<ErrorEvent, UUID> {

    Optional<ErrorEvent> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    Page<ErrorEvent> findByGroupIdOrderByOccurredAtDesc(UUID groupId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT e.userEmail) FROM ErrorEvent e WHERE e.group.id = :groupId AND e.userEmail IS NOT NULL")
    long countDistinctUsersByGroupId(@Param("groupId") UUID groupId);

    /** Liste sayfası için toplu etkilenen-kullanıcı sayıları: [groupId, count] satırları döner. */
    @Query("""
            SELECT e.group.id, COUNT(DISTINCT e.userEmail) FROM ErrorEvent e
            WHERE e.group.id IN :groupIds AND e.userEmail IS NOT NULL
            GROUP BY e.group.id
            """)
    List<Object[]> countDistinctUsersByGroupIds(@Param("groupIds") Collection<UUID> groupIds);
}
