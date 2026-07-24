package com.scrumtools.dto;

import com.scrumtools.entity.CiConnection;

import java.time.LocalDateTime;
import java.util.UUID;

/** CI/CD bağlantısı yanıtı — token asla dönülmez, sadece tokenHint. */
public record CiConnectionResponse(
        UUID id,
        String provider,
        String name,
        String baseUrl,
        String username,
        String tokenHint,
        String status,
        String serverVersion,
        boolean crumbEnabled,
        String createdBy,
        LocalDateTime lastValidatedAt,
        long mappingCount,
        LocalDateTime createdAt
) {
    public static CiConnectionResponse from(CiConnection c, long mappingCount) {
        return new CiConnectionResponse(
                c.getId(),
                c.getProvider().name(),
                c.getName(),
                c.getBaseUrl(),
                c.getUsername(),
                c.getTokenHint(),
                c.getStatus().name(),
                c.getServerVersion(),
                Boolean.TRUE.equals(c.getCrumbEnabled()),
                c.getCreatedBy(),
                c.getLastValidatedAt(),
                mappingCount,
                c.getCreatedAt()
        );
    }
}
