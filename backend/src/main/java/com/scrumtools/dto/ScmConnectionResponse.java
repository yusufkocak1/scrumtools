package com.scrumtools.dto;

import com.scrumtools.entity.ScmConnection;

import java.time.LocalDateTime;
import java.util.UUID;

/** SCM bağlantısı yanıtı — token asla dönülmez, sadece tokenHint. */
public record ScmConnectionResponse(
        UUID id,
        String provider,
        String name,
        String baseUrl,
        String tokenHint,
        String username,
        String status,
        String createdBy,
        LocalDateTime lastValidatedAt,
        long repoCount,
        LocalDateTime createdAt
) {
    public static ScmConnectionResponse from(ScmConnection c, long repoCount) {
        return new ScmConnectionResponse(
                c.getId(),
                c.getProvider().name(),
                c.getName(),
                c.getBaseUrl(),
                c.getTokenHint(),
                c.getUsername(),
                c.getStatus().name(),
                c.getCreatedBy(),
                c.getLastValidatedAt(),
                repoCount,
                c.getCreatedAt()
        );
    }
}
