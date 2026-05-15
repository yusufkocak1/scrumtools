package com.scrumtools.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Oyuncunun cevap gönderme isteği.
 */
public record QuizAnswerRequest(
        @NotNull String questionId,
        @NotNull @Min(0) Integer selectedOptionIndex,
        @NotNull Long answeredInMs
) {
}

