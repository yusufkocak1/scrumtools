package com.scrumtools.dto;

import com.scrumtools.entity.ScmPullRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/** Task'a bağlı pull request yanıtı (DevPanel). */
public record ScmPullRequestResponse(
        UUID id,
        UUID branchId,
        UUID repositoryId,
        String externalId,
        String title,
        String sourceBranch,
        String targetBranch,
        String state,
        String webUrl,
        boolean draft,
        String createdBy,
        String repositoryName,
        String provider,
        LocalDateTime mergedAt,
        LocalDateTime createdAt
) {
    public static ScmPullRequestResponse from(ScmPullRequest pr) {
        return new ScmPullRequestResponse(
                pr.getId(),
                pr.getBranch() != null ? pr.getBranch().getId() : null,
                pr.getRepository().getId(),
                pr.getExternalId(),
                pr.getTitle(),
                pr.getSourceBranch(),
                pr.getTargetBranch(),
                pr.getState().name(),
                pr.getWebUrl(),
                pr.isDraft(),
                pr.getCreatedBy(),
                pr.getRepository().getName(),
                pr.getRepository().getConnection().getProvider().name(),
                pr.getMergedAt(),
                pr.getCreatedAt()
        );
    }
}
