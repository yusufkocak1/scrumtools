package com.scrumtools.dto;

import com.scrumtools.entity.ScmCommit;

import java.time.LocalDateTime;
import java.util.UUID;

/** Task'a bağlı commit yanıtı (DevPanel). */
public record ScmCommitResponse(
        UUID id,
        String sha,
        String shortMessage,
        String authorName,
        String authorEmail,
        LocalDateTime authoredAt,
        String webUrl,
        String repositoryName,
        String branchHint
) {
    public static ScmCommitResponse from(ScmCommit c) {
        return new ScmCommitResponse(
                c.getId(),
                c.getSha(),
                c.getShortMessage(),
                c.getAuthorName(),
                c.getAuthorEmail(),
                c.getAuthoredAt(),
                c.getWebUrl(),
                c.getRepository().getName(),
                c.getBranchHint()
        );
    }
}
