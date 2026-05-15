package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DocPageRequest(
        @NotBlank(message = "Sayfa başlığı boş olamaz")
        @Size(max = 500)
        String title,

        String content,

        UUID parentPageId,

        Integer sortOrder,

        String changeSummary
) {}

