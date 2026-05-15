package com.scrumtools.dto;

import com.scrumtools.entity.enums.InvitationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvitationRequest(
        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi giriniz")
        String email,

        @NotNull(message = "Davet tipi boş olamaz")
        InvitationType type,

        @NotNull(message = "Hedef ID boş olamaz")
        UUID targetId,

        UUID roleId
) {}

