package com.scrumtools.repository;

import com.scrumtools.entity.ScmConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScmConnectionRepository extends JpaRepository<ScmConnection, UUID> {

    List<ScmConnection> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    Optional<ScmConnection> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}
