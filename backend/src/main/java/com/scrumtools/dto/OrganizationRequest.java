package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank(message = "Organizasyon adı boş olamaz")
        @Size(min = 2, max = 100)
        String name,

        @NotBlank(message = "Slug boş olamaz")
        @Size(min = 2, max = 50)
        String slug,

        String description,
        String logoUrl
) {}

