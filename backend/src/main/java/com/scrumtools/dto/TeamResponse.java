package com.scrumtools.dto;

import com.scrumtools.entity.Team;
import com.scrumtools.entity.TeamMember;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend uyumluluğu için Firestore formatına benzer response.
 * Frontend şunu bekler:
 * - id, teamName, teamCode, adminEmail
 * - members: { email: { displayName, role, skills[] } }
 * - memberEmails: [email1, email2, ...]
 */
public record TeamResponse(
        String id,
        String teamName,
        String teamCode,
        String adminEmail,
        String organizationId,
        Map<String, TeamMemberResponse> members,
        List<String> memberEmails
) {
    public static TeamResponse from(Team team) {
        Map<String, TeamMemberResponse> membersMap = new LinkedHashMap<>();
        List<String> emails = new java.util.ArrayList<>();

        for (TeamMember member : team.getMembers()) {
            membersMap.put(member.getEmail(), TeamMemberResponse.from(member));
            emails.add(member.getEmail());
        }

        return new TeamResponse(
                team.getId().toString(),
                team.getTeamName(),
                team.getTeamCode(),
                team.getAdminEmail(),
                team.getOrganization() != null ? team.getOrganization().getId().toString() : null,
                membersMap,
                emails
        );
    }
}
