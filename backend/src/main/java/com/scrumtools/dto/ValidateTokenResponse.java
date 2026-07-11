package com.scrumtools.dto;

import com.scrumtools.entity.enums.TokenPurpose;

public record ValidateTokenResponse(
        boolean valid,
        String email,
        String name,
        TokenPurpose purpose
) {
    public static ValidateTokenResponse invalid() {
        return new ValidateTokenResponse(false, null, null, null);
    }
}
