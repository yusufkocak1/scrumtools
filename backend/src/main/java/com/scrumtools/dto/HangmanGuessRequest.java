package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Harf ya da kelime tahmini.
 * Harf tahmininde tek karakter, kelime tahmininde tam kelime beklenir.
 */
public record HangmanGuessRequest(
        @NotBlank String value
) {
}
