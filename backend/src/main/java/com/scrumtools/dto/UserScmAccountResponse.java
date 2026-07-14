package com.scrumtools.dto;

import com.scrumtools.entity.UserScmAccount;

import java.time.LocalDateTime;
import java.util.UUID;

/** Kişisel SCM hesabı yanıtı — token asla dönmez, sadece tokenHint. */
public record UserScmAccountResponse(
        UUID id,
        String provider,
        String baseUrl,
        String username,
        String providerEmail,
        String tokenHint,
        String status,
        LocalDateTime createdAt
) {
    public static UserScmAccountResponse from(UserScmAccount a) {
        return new UserScmAccountResponse(
                a.getId(),
                a.getProvider().name(),
                a.getBaseUrl(),
                a.getUsername(),
                a.getProviderEmail(),
                a.getTokenHint(),
                a.getStatus().name(),
                a.getCreatedAt()
        );
    }
}
