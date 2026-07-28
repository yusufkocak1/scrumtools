package com.scrumtools.repository;

import com.scrumtools.entity.ScmPullRequest;
import com.scrumtools.entity.enums.ScmPullRequestState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScmPullRequestRepository extends JpaRepository<ScmPullRequest, UUID> {

    List<ScmPullRequest> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    Optional<ScmPullRequest> findByRepositoryIdAndTaskIdAndExternalId(
            UUID repositoryId, UUID taskId, String externalId);

    List<ScmPullRequest> findByBranchIdAndState(UUID branchId, ScmPullRequestState state);

    List<ScmPullRequest> findByRepositoryId(UUID repositoryId);

    void deleteByRepositoryId(UUID repositoryId);
}
