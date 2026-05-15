package com.scrumtools.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Quiz şablonu oluşturma / güncelleme isteği.
 */
public record QuizTemplateRequest(
        @NotBlank String title,
        String description,
        @NotEmpty @Valid List<QuizQuestionRequest> questions
) {
}

