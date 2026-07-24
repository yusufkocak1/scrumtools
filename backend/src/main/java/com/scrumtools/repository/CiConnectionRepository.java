package com.scrumtools.repository;

import com.scrumtools.entity.CiConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CiConnectionRepository extends JpaRepository<CiConnection, UUID> {

    List<CiConnection> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    Optional<CiConnection> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}
