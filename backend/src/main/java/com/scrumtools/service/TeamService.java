package com.scrumtools.service;

import com.scrumtools.dto.TeamRequest;
import com.scrumtools.dto.TeamResponse;
import com.scrumtools.dto.UpdateMemberRoleRequest;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.TeamMember;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    // ─── Get Teams By Organisation ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByOrg(UUID orgId, String query) {
        List<Team> teams;
        if (query != null && !query.isBlank()) {
            teams = teamRepository.searchByOrganizationIdAndName(orgId, query.trim());
        } else {
            teams = teamRepository.findByOrganizationId(orgId);
        }
        return teams.stream().map(TeamResponse::from).toList();
    }

    // ─── Create Team (only ORG_OWNER / ORG_ADMIN) ────────────────────────────

    @Transactional
    public TeamResponse createTeam(UUID orgId, TeamRequest request, String adminEmail) {
        // Yetki kontrolü: sadece org admin/owner oluşturabilir
        checkOrgAdminAccess(orgId, adminEmail);

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));

        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) {
            User user = userRepository.findByEmail(adminEmail).orElse(null);
            displayName = user != null ? user.getName() : adminEmail;
        }

        Team team = Team.builder()
                .teamName(request.teamName().trim())
                .teamCode(request.teamCode().trim().toUpperCase())
                .adminEmail(adminEmail)
                .organization(org)
                .members(new ArrayList<>())
                .build();

        // Admin'i ilk üye olarak ekle
        User adminUser = userRepository.findByEmail(adminEmail).orElse(null);
        TeamMember adminMember = TeamMember.builder()
                .team(team)
                .user(adminUser)
                .email(adminEmail)
                .displayName(displayName)
                .role("admin")
                .skills(new ArrayList<>())
                .build();

        team.getMembers().add(adminMember);
        team = teamRepository.save(team);

        log.info("Yeni takım oluşturuldu: {} (org={}, code={}, admin={})",
                team.getTeamName(), orgId, team.getTeamCode(), adminEmail);
        return TeamResponse.from(team);
    }

    // ─── Get My Teams ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(String email) {
        List<Team> teams = teamRepository.findByMemberEmail(email);
        return teams.stream().map(TeamResponse::from).toList();
    }

    // ─── Get Team By ID ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));
        return TeamResponse.from(team);
    }

    // ─── Add Member To Team (ORG_OWNER / ORG_ADMIN veya takım admini) ─────────

    @Transactional
    public TeamResponse addMemberToTeam(UUID orgId, UUID teamId, String targetEmail, String requesterEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        // Takım bu organizasyona ait mi kontrol et
        if (team.getOrganization() == null || !team.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Bu takım belirtilen organizasyona ait değil.");
        }

        // Yetki: org admin/owner VEYA takım admini
        boolean isOrgAdmin = organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(
                orgId, requesterEmail, List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN));
        boolean isTeamAdmin = teamMemberRepository.findByTeamIdAndEmail(teamId, requesterEmail)
                .map(m -> "admin".equalsIgnoreCase(m.getRole()))
                .orElse(false);

        if (!isOrgAdmin && !isTeamAdmin) {
            throw new SecurityException("Takıma üye ekleme yetkisine sahip değilsiniz.");
        }

        // Hedef kullanıcı organizasyon üyesi mi?
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + targetEmail));
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, targetUser.getId())) {
            throw new IllegalArgumentException("Eklenecek kullanıcı bu organizasyonun üyesi değil.");
        }

        // Zaten takımda mı?
        if (teamMemberRepository.existsByTeamIdAndEmail(teamId, targetEmail)) {
            throw new IllegalArgumentException("Kullanıcı zaten bu takımda.");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(targetUser)
                .email(targetEmail)
                .displayName(targetUser.getName())
                .role("member")
                .skills(new ArrayList<>())
                .build();

        team.getMembers().add(member);
        team = teamRepository.save(team);

        log.info("Kullanıcı takıma eklendi: {} → {} (org={})", targetEmail, team.getTeamName(), orgId);
        return TeamResponse.from(team);
    }

    // ─── Remove User From Team ────────────────────────────────────────────────

    @Transactional
    public TeamResponse removeUserFromTeam(UUID teamId, String email) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        if (email.equalsIgnoreCase(team.getAdminEmail())) {
            throw new IllegalArgumentException("Takım yöneticisi kendini takımdan kaldıramaz.");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndEmail(teamId, email)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı: " + email));

        team.getMembers().remove(member);
        team = teamRepository.save(team);

        log.info("Kullanıcı takımdan çıkarıldı: {} ← {}", email, team.getTeamName());
        return TeamResponse.from(team);
    }

    // ─── Update Member Role & Skills ──────────────────────────────────────────

    @Transactional
    public TeamResponse updateMemberRole(UUID teamId, String email, UpdateMemberRoleRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        TeamMember member = teamMemberRepository.findByTeamIdAndEmail(teamId, email)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı: " + email));

        member.setRole(request.role());
        if (request.skills() != null) {
            member.setSkills(new ArrayList<>(request.skills()));
        }

        teamMemberRepository.save(member);
        team = teamRepository.findById(teamId).orElseThrow();
        log.info("Üye bilgileri güncellendi: {} (role={}, team={})", email, request.role(), team.getTeamName());
        return TeamResponse.from(team);
    }

    // ─── Update Team Info (name, code) ───────────────────────────────────────

    @Transactional
    public TeamResponse updateTeam(UUID teamId, TeamRequest request, String requesterEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        // Yetki: sadece takım admini düzenleyebilir
        if (!team.getAdminEmail().equalsIgnoreCase(requesterEmail)) {
            throw new SecurityException("Takım bilgilerini güncellemek için takım yöneticisi olmanız gerekir.");
        }

        team.setTeamName(request.teamName().trim());
        team.setTeamCode(request.teamCode().trim().toUpperCase());
        team = teamRepository.save(team);

        log.info("Takım bilgileri güncellendi: {} (code={}, admin={})",
                team.getTeamName(), team.getTeamCode(), requesterEmail);
        return TeamResponse.from(team);
    }

    // ─── Link Team To Project (release yönetimi için) ─────────────────────────

    @Transactional
    public TeamResponse linkProject(UUID teamId, UUID projectId, String requesterEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        UUID orgId = team.getOrganization().getId();

        // Yetki: org admin/owner VEYA takım admini
        boolean isOrgAdmin = organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(
                orgId, requesterEmail, List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN));
        boolean isTeamAdmin = team.getAdminEmail().equalsIgnoreCase(requesterEmail);
        if (!isOrgAdmin && !isTeamAdmin) {
            throw new SecurityException("Takımı projeye bağlamak için takım veya organizasyon yöneticisi olmanız gerekir.");
        }

        if (projectId == null) {
            team.setProject(null);
        } else {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
            if (project.getOrganization() == null || !project.getOrganization().getId().equals(orgId)) {
                throw new IllegalArgumentException("Proje, takımın organizasyonuna ait değil.");
            }
            team.setProject(project);

            // Takımın projesiz görevleri projeye bağlanır (customId'leri korunur;
            // proje bağı olan görevler dokunulmaz — task artık projeye aittir).
            int migrated = taskRepository.assignProjectToTeamTasks(teamId, project);
            if (migrated > 0) {
                log.info("Takım projeye bağlandı: {} görev {} projesine taşındı.", migrated, project.getKey());
            }
        }

        team = teamRepository.save(team);
        log.info("Takım proje bağlantısı güncellendi: {} → {} (istek: {})",
                team.getTeamName(), projectId, requesterEmail);
        return TeamResponse.from(team);
    }

    // ─── Update Display Name Across All Teams ─────────────────────────────────

    @Transactional
    public void updateDisplayNameAcrossTeams(String email, String newDisplayName) {
        List<TeamMember> members = teamMemberRepository.findByEmail(email);
        for (TeamMember member : members) {
            member.setDisplayName(newDisplayName);
        }
        teamMemberRepository.saveAll(members);
        log.info("Display name tüm takımlarda güncellendi: {} → {}", email, newDisplayName);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void checkOrgAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(
                orgId, email, List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Takım oluşturmak için organizasyon yöneticisi olmanız gerekir.");
        }
    }
}

