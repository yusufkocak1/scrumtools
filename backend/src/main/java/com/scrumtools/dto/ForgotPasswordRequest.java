package com.scrumtools.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi girin")
        String email
) {}
