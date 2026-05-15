package com.scrumtools.dto;

import com.scrumtools.entity.TeamMember;

import java.util.List;

public record TeamMemberResponse(
        String displayName,
        String role,
        List<String> skills
) {
    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getDisplayName(),
                member.getRole(),
                member.getSkills() != null ? member.getSkills() : List.of()
        );
    }
}

