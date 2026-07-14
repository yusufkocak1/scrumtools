package com.scrumtools.repository;

import com.scrumtools.entity.ScmBranch;
import com.scrumtools.entity.enums.ScmBranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScmBranchRepository extends JpaRepository<ScmBranch, UUID> {

    List<ScmBranch> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    List<ScmBranch> findByRepositoryIdAndName(UUID repositoryId, String name);

    Optional<ScmBranch> findByRepositoryIdAndTaskIdAndName(UUID repositoryId, UUID taskId, String name);

    List<ScmBranch> findByStatus(ScmBranchStatus status);

    void deleteByRepositoryId(UUID repositoryId);
}
