package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRequest(
        @NotBlank(message = "Takım adı boş olamaz")
        String teamName,

        @NotBlank(message = "Takım kodu boş olamaz")
        @Size(min = 2, max = 4, message = "Takım kodu 2-4 karakter olmalıdır")
        String teamCode,

        String displayName
) {}

