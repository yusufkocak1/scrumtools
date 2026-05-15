package com.scrumtools.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Quiz sorusu oluşturma / güncelleme isteği.
 */
public record QuizQuestionRequest(
        @NotBlank String questionText,
        @NotEmpty @Size(min = 2, max = 6) List<@NotBlank String> options,
        @NotNull @Min(0) Integer correctOptionIndex,
        @NotNull @Min(5) @Max(120) Integer timeLimitSeconds
) {
}

