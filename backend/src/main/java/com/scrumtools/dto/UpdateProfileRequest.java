package com.scrumtools.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100, message = "Ad 2-100 karakter olmalıdır")
        String name,

        String avatarUrl,

        @Size(max = 20, message = "Telefon numarası en fazla 20 karakter olabilir")
        String phone,

        String timezone,

        @Size(min = 2, max = 10)
        String locale
) {}

