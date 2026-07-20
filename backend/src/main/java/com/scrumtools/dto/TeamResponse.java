package com.scrumtools.dto;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.TeamMember;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        /** Birincil (varsayılan) proje — görev oluşturulurken ön seçili gelir. */
        String projectId,
        String projectName,
        /** Takımın çalıştığı tüm projeler; birincil proje başta. */
        List<TeamProjectResponse> projects,
        Map<String, TeamMemberResponse> members,
        List<String> memberEmails
) {
    /** Takım proje seçicisi için minimal proje bilgisi. */
    public record TeamProjectResponse(String id, String name, String key, String color, boolean primary) {}

    public static TeamResponse from(Team team) {
        Map<String, TeamMemberResponse> membersMap = new LinkedHashMap<>();
        List<String> emails = new java.util.ArrayList<>();

        for (TeamMember member : team.getMembers()) {
            membersMap.put(member.getEmail(), TeamMemberResponse.from(member));
            emails.add(member.getEmail());
        }

        Project primary = team.getProject();
        UUID primaryId = primary != null ? primary.getId() : null;

        // Birincil proje her zaman listenin başında — frontend varsayılan seçimi buradan alır.
        List<TeamProjectResponse> projects = team.getProjects().stream()
                .sorted(Comparator
                        .comparing((Project p) -> !p.getId().equals(primaryId))
                        .thenComparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new TeamProjectResponse(
                        p.getId().toString(), p.getName(), p.getKey(), p.getColor(),
                        p.getId().equals(primaryId)))
                .toList();

        return new TeamResponse(
                team.getId().toString(),
                team.getTeamName(),
                team.getTeamCode(),
                team.getAdminEmail(),
                team.getOrganization() != null ? team.getOrganization().getId().toString() : null,
                primary != null ? primary.getId().toString() : null,
                primary != null ? primary.getName() : null,
                projects,
                membersMap,
                emails
        );
    }
}
