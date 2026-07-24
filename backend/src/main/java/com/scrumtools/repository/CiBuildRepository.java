package com.scrumtools.repository;

import com.scrumtools.entity.CiBuild;
import com.scrumtools.entity.enums.CiBuildStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CiBuildRepository extends JpaRepository<CiBuild, UUID> {

    List<CiBuild> findByTaskIdOrderByTriggeredAtDesc(UUID taskId);

    List<CiBuild> findByReleaseIdOrderByTriggeredAtDesc(UUID releaseId);

    /**
     * Poller'ın iş listesi: terminal olmayan build'ler, bağlantı ve eşleme
     * fetch edilerek (client kurmak için gerekli, N+1 önlenir).
     */
    @Query("SELECT b FROM CiBuild b JOIN FETCH b.jobMapping m JOIN FETCH m.connection " +
           "WHERE b.status IN :statuses ORDER BY b.triggeredAt ASC")
    List<CiBuild> findActiveWithConnection(@Param("statuses") Collection<CiBuildStatus> statuses);

    @Query("SELECT b FROM CiBuild b WHERE b.jobMapping.project.id = :projectId " +
           "AND (:status IS NULL OR b.status = :status)")
    Page<CiBuild> findByProject(@Param("projectId") UUID projectId,
                                @Param("status") CiBuildStatus status,
                                Pageable pageable);

    /** Çift tık / rate limit — aynı task için son X dakikadaki tetikleme sayısı. */
    long countByTaskIdAndTriggeredAtAfter(UUID taskId, LocalDateTime after);

    /** Çift tık / rate limit — aynı release için son X dakikadaki tetikleme sayısı. */
    long countByReleaseIdAndTriggeredAtAfter(UUID releaseId, LocalDateTime after);

    void deleteByJobMappingId(UUID jobMappingId);
}
