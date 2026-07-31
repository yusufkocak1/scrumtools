package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * @param category HangmanCategory kodu (ör. ANIMALS) — kelimeler bir kategoriye eklenir.
 */
public record HangmanWordBulkRequest(
        @NotBlank String language,
        @NotBlank String category,
        @NotEmpty List<@NotBlank String> words
) {}
