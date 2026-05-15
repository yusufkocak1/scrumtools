package com.scrumtools.dto;

import com.scrumtools.entity.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "Proje adı boş olamaz")
        @Size(min = 2, max = 100)
        String name,

        @NotBlank(message = "Proje anahtarı boş olamaz")
        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[A-Z0-9]+$", message = "Proje anahtarı sadece büyük harf ve rakam içerebilir")
        String key,

        String description,
        ProjectType projectType,
        String iconUrl,
        String color
) {}

