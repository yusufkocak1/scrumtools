package com.scrumtools.repository;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /** Task customId sayacını güncellerken race condition'a karşı kilitli okuma. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") UUID id);

    List<Project> findByOrganizationId(UUID organizationId);

    List<Project> findByOrganizationIdAndStatus(UUID organizationId, ProjectStatus status);

    boolean existsByOrganizationIdAndKey(UUID organizationId, String key);

    long countByOrganizationIdAndStatus(UUID organizationId, ProjectStatus status);

    Optional<Project> findByOrganizationIdAndKey(UUID organizationId, String key);

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON pm.project = p WHERE pm.user.email = :email AND p.status = 'ACTIVE'")
    List<Project> findAllActiveByMemberEmail(@Param("email") String email);
}

