package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateNameRequest(
        @NotBlank(message = "İsim boş olamaz")
        String name
) {}

