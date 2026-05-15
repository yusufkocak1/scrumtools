package com.scrumtools.repository;

import com.scrumtools.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {
    List<WorkflowStatus> findByWorkflowIdOrderByPositionAsc(UUID workflowId);
    Optional<WorkflowStatus> findByWorkflowIdAndIsInitialTrue(UUID workflowId);
}

