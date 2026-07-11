package com.scrumtools.dto;

import com.scrumtools.entity.enums.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi girin")
        String email,

        @NotBlank(message = "İsim boş olamaz")
        String name,

        OrgRole orgRole
) {}
