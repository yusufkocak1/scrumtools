package com.scrumtools.dto;

import com.scrumtools.entity.ScmBranch;

import java.time.LocalDateTime;
import java.util.UUID;

/** Task'a bağlı branch yanıtı (DevPanel). */
public record ScmBranchResponse(
        UUID id,
        String name,
        String webUrl,
        String status,
        boolean createdViaApp,
        String createdBy,
        String repositoryName,
        LocalDateTime createdAt
) {
    public static ScmBranchResponse from(ScmBranch b) {
        return new ScmBranchResponse(
                b.getId(),
                b.getName(),
                b.getWebUrl(),
                b.getStatus().name(),
                b.isCreatedViaApp(),
                b.getCreatedBy(),
                b.getRepository().getName(),
                b.getCreatedAt()
        );
    }
}
