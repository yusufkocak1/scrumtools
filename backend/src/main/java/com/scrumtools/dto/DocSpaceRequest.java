package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocSpaceRequest(
        @NotBlank(message = "Space adı boş olamaz")
        @Size(max = 255)
        String name,

        String description
) {}

