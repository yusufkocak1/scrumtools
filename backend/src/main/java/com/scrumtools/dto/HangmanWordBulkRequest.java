package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HangmanWordBulkRequest(
        @NotBlank String language,
        @NotEmpty List<@NotBlank String> words
) {}
