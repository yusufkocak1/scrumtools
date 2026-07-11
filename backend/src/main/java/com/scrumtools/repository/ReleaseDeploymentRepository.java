package com.scrumtools.repository;

import com.scrumtools.entity.ReleaseDeployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReleaseDeploymentRepository extends JpaRepository<ReleaseDeployment, UUID> {

    List<ReleaseDeployment> findByReleaseIdOrderByTaskCustomIdAsc(UUID releaseId);

    List<ReleaseDeployment> findByTaskIdOrderByReleasedAtDesc(UUID taskId);

    boolean existsByReleaseId(UUID releaseId);
}
