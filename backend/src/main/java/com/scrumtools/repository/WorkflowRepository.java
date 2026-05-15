package com.scrumtools.repository;

import com.scrumtools.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByProjectId(UUID projectId);
    List<Workflow> findByTeamId(UUID teamId);
    Optional<Workflow> findByProjectIdAndIsDefaultTrue(UUID projectId);
    Optional<Workflow> findByTeamIdAndIsDefaultTrue(UUID teamId);
}

