package com.scrumtools.repository;

import com.scrumtools.entity.ErrorGroup;
import com.scrumtools.entity.enums.ErrorGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ErrorGroupRepository extends JpaRepository<ErrorGroup, UUID> {

    Optional<ErrorGroup> findByFingerprint(String fingerprint);

    /** Atomik sayaç artırma — eşzamanlı olaylarda kayıp güncelleme olmaz. */
    @Modifying
    @Transactional
    @Query("UPDATE ErrorGroup g SET g.occurrenceCount = g.occurrenceCount + 1, g.lastSeenAt = :now WHERE g.id = :id")
    void incrementOccurrence(@Param("id") UUID id, @Param("now") LocalDateTime now);

    Page<ErrorGroup> findByStatusOrderByLastSeenAtDesc(ErrorGroupStatus status, Pageable pageable);

    Page<ErrorGroup> findAllByOrderByLastSeenAtDesc(Pageable pageable);
}
