package com.scrumtools.dto;

import com.scrumtools.entity.enums.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateMemberRequest(
        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi girin")
        String email,

        @NotBlank(message = "İsim boş olamaz")
        String name,

        OrgRole orgRole,

        /**
         * Opsiyonel: üye aynı anda bu takımlara da eklenir. Takım projelere
         * bağlıysa üye o projelere de otomatik düşer — davet eden kişinin
         * ayrıca takım ve proje ekranlarında gezinmesi gerekmez.
         */
        List<UUID> teamIds
) {
    public CreateMemberRequest(String email, String name, OrgRole orgRole) {
        this(email, name, orgRole, List.of());
    }

    public List<UUID> teamIds() {
        return teamIds != null ? teamIds : List.of();
    }
}
