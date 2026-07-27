package com.scrumtools.dto;

import com.scrumtools.entity.ProjectTeam;
import com.scrumtools.entity.enums.MemberType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Projeye bağlı bir takım. Bağ kalıcıdır: takıma sonradan katılan üyeler bu
 * ayarlarla ({@code roles}, {@code memberType}) otomatik olarak projeye eklenir.
 */
public record ProjectTeamResponse(
        UUID id,
        UUID teamId,
        String teamName,
        String teamCode,
        int memberCount,
        List<ProjectMemberResponse.RoleInfo> roles,
        MemberType memberType,
        String addedByName,
        LocalDateTime createdAt
) {
    public static ProjectTeamResponse from(ProjectTeam link, int memberCount) {
        return new ProjectTeamResponse(
                link.getId(),
                link.getTeam().getId(),
                link.getTeam().getTeamName(),
                link.getTeam().getTeamCode(),
                memberCount,
                link.getRoles().stream()
                        .map(r -> new ProjectMemberResponse.RoleInfo(r.getId(), r.getName(), r.getColor()))
                        .toList(),
                link.getMemberType(),
                link.getAddedBy() != null ? link.getAddedBy().getName() : null,
                link.getCreatedAt()
        );
    }
}
