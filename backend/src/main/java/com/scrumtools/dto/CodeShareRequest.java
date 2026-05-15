package com.scrumtools.dto;

import jakarta.validation.constraints.NotNull;

public record CodeShareRequest(
        @NotNull String data
) {
}

