package com.scrumtools.repository;

import com.scrumtools.entity.ScmRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScmRepositoryRepository extends JpaRepository<ScmRepository, UUID> {

    List<ScmRepository> findByProjectId(UUID projectId);

    List<ScmRepository> findByConnectionId(UUID connectionId);

    long countByConnectionId(UUID connectionId);

    Optional<ScmRepository> findByConnectionIdAndExternalId(UUID connectionId, String externalId);

    Optional<ScmRepository> findByIdAndProjectId(UUID id, UUID projectId);

    /** Org genelinde eşlenmiş repo sayısı — paket limiti kontrolü için. */
    @Query("SELECT COUNT(r) FROM ScmRepository r WHERE r.connection.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
