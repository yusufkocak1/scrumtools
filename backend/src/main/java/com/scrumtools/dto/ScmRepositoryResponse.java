package com.scrumtools.dto;

import com.scrumtools.entity.ScmRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/** Projeye eşlenmiş repo yanıtı. */
public record ScmRepositoryResponse(
        UUID id,
        UUID connectionId,
        String connectionName,
        String provider,
        String name,
        String fullName,
        String defaultBranch,
        String webUrl,
        String webhookStatus,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt
) {
    public static ScmRepositoryResponse from(ScmRepository r) {
        return new ScmRepositoryResponse(
                r.getId(),
                r.getConnection().getId(),
                r.getConnection().getName(),
                r.getConnection().getProvider().name(),
                r.getName(),
                r.getFullName(),
                r.getDefaultBranch(),
                r.getWebUrl(),
                r.getWebhookStatus().name(),
                r.getLastSyncedAt(),
                r.getCreatedAt()
        );
    }
}
