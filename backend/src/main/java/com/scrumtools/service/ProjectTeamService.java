package com.scrumtools.service;

import com.scrumtools.entity.Project;
import com.scrumtools.entity.ProjectMember;
import com.scrumtools.entity.ProjectTeam;
import com.scrumtools.entity.Role;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.TeamMember;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.MemberType;
import com.scrumtools.entity.enums.ProjectStatus;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.repository.ProjectTeamRepository;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Proje ↔ takım bağının ve bu bağdan doğan proje üyeliklerinin tek sahibi.
 * <p>
 * Bağ kurulduğunda takımın o anki üyeleri projeye eklenir; bağ yaşadığı sürece
 * takım üyeliğindeki her değişiklik projeye yansır (tam senkron):
 * <ul>
 *   <li>takıma katılan → bağlı tüm projelere eklenir,</li>
 *   <li>takımdan çıkan → aynı projeye bağlı başka bir takımda değilse projeden çıkarılır.</li>
 * </ul>
 * Elle eklenmiş üyeler ({@code sourceTeam == null}) ve proje lideri bu otomatik
 * çıkarmadan muaftır — takım hareketi kimsenin elle verilmiş erişimini silmemeli.
 * <p>
 * {@code ProjectService} ve {@code TeamService} her iki yönden de buraya delege eder;
 * böylece bağ ve üyelik mantığı tek yerde kalır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTeamService {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // ─── Bağ kurma / çözme ────────────────────────────────────────────────────

    /**
     * Takımı projeye bağlar (varsa rol/üyelik türü ayarlarını günceller) ve takımın
     * o anki tüm üyelerini projeye ekler. Zaten projede olan üyeler atlanır.
     *
     * @return bu çağrıda projeye yeni eklenen üyeler
     */
    @Transactional
    public List<ProjectMember> linkTeam(Project project, Team team, Set<Role> roles,
                                        MemberType memberType, User requester) {
        if (team.getOrganization() == null || project.getOrganization() == null
                || !team.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw new IllegalArgumentException("Takım ve proje aynı organizasyona ait olmalı.");
        }

        ProjectTeam link = projectTeamRepository
                .findByProjectIdAndTeamId(project.getId(), team.getId())
                .orElseGet(() -> ProjectTeam.builder()
                        .project(project)
                        .team(team)
                        .addedBy(requester)
                        .build());

        // null = "bu çağrının söyleyecek bir şeyi yok": takım ekranından yapılan
        // bağlama, proje ekranında seçilmiş rol/üyelik türü ayarlarını silmemeli.
        if (roles != null) link.setRoles(new HashSet<>(roles));
        if (memberType != null) link.setMemberType(memberType);
        projectTeamRepository.save(link);

        Set<Role> linkRoles = link.getRoles();
        MemberType type = link.getMemberType();

        // Takım artık bu projede çalışıyor — görev/commit eşleştirmesi bu kümeye bakar.
        if (team.getProjects().stream().noneMatch(p -> p.getId().equals(project.getId()))) {
            team.getProjects().add(project);
            teamRepository.save(team);
        }

        List<ProjectMember> added = new ArrayList<>();
        for (TeamMember tm : teamMemberRepository.findByTeamId(team.getId())) {
            attachMemberToProject(project, team, tm.getEmail(), linkRoles, type, requester)
                    .ifPresent(added::add);
        }

        log.info("Takım projeye bağlandı: {} → {} ({} yeni üye)",
                team.getTeamName(), project.getKey(), added.size());
        return added;
    }

    /**
     * Takım–proje bağını çözer ve bu bağla gelmiş üyeleri projeden çıkarır.
     * Takımın o projede görevi varsa reddedilir — görevler önce taşınmalı.
     */
    @Transactional
    public void unlinkTeam(Project project, Team team) {
        long taskCount = taskRepository.countByTeamIdAndProjectId(team.getId(), project.getId());
        if (taskCount > 0) {
            throw new IllegalArgumentException(
                    "Bu projede takımın " + taskCount + " görevi var. Takımı projeden çıkarmadan önce "
                            + "görevleri başka bir projeye taşıyın.");
        }

        for (TeamMember tm : teamMemberRepository.findByTeamId(team.getId())) {
            detachMemberFromProject(project, team, tm.getEmail());
        }

        projectTeamRepository.deleteByProjectIdAndTeamId(project.getId(), team.getId());

        team.getProjects().removeIf(p -> p.getId().equals(project.getId()));
        if (team.getProject() != null && team.getProject().getId().equals(project.getId())) {
            // Birincil proje kaldırıldı — kalanlardan biri birincil olur.
            team.setProject(team.getProjects().stream().findFirst().orElse(null));
        }
        teamRepository.save(team);

        log.info("Takım projeden ayrıldı: {} ⊘ {}", team.getTeamName(), project.getKey());
    }

    @Transactional(readOnly = true)
    public List<ProjectTeam> getLinksByProject(UUID projectId) {
        return projectTeamRepository.findByProjectId(projectId);
    }

    // ─── Takım üyeliği değişiklik kancaları ───────────────────────────────────

    /** Takıma yeni üye katıldı — bağlı tüm projelere ekle. */
    @Transactional
    public void onTeamMemberAdded(Team team, String email) {
        for (ProjectTeam link : projectTeamRepository.findByTeamId(team.getId())) {
            attachMemberToProject(link.getProject(), team, email,
                    link.getRoles(), link.getMemberType(), link.getAddedBy());
        }
    }

    /** Üye takımdan çıkarıldı — başka bağlı takımda değilse projelerden de çıkar. */
    @Transactional
    public void onTeamMemberRemoved(Team team, String email) {
        for (ProjectTeam link : projectTeamRepository.findByTeamId(team.getId())) {
            detachMemberFromProject(link.getProject(), team, email);
        }
    }

    /** Takım silinmeden/organizasyondan çıkarılmadan önce tüm bağları temizler. */
    @Transactional
    public void onTeamMemberRemovedFromOrg(String email) {
        for (TeamMember tm : teamMemberRepository.findByEmail(email)) {
            onTeamMemberRemoved(tm.getTeam(), email);
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private Optional<ProjectMember> attachMemberToProject(Project project, Team team, String email,
                                                          Set<Role> roles, MemberType memberType,
                                                          User addedBy) {
        if (project.getStatus() == ProjectStatus.DELETED) return Optional.empty();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return Optional.empty(); // hesabı olmayan legacy takım üyesi

        Optional<ProjectMember> existing =
                projectMemberRepository.findByProjectIdAndUserId(project.getId(), user.getId());
        if (existing.isPresent()) {
            // Zaten üye — rolleri/türü ezmeyiz. Elle eklenmişse kaynağı da elle kalır:
            // takım bağı çözüldüğünde erişimini kaybetmemeli.
            return Optional.empty();
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .roles(new HashSet<>(roles))
                .memberType(memberType != null ? memberType : MemberType.MEMBER)
                .addedBy(addedBy)
                .sourceTeam(team)
                .build();
        return Optional.of(projectMemberRepository.save(member));
    }

    private void detachMemberFromProject(Project project, Team team, String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), user.getId())
                .orElse(null);
        if (member == null) return;

        // Elle eklenmiş ya da başka bir takımın getirdiği üyelik — bu takımın işi değil.
        if (member.getSourceTeam() == null || !member.getSourceTeam().getId().equals(team.getId())) return;

        // Proje lideri takım hareketiyle projeden düşmemeli; üyeliği elle eklenmiş sayılır.
        if (project.getLead() != null && project.getLead().getId().equals(user.getId())) {
            member.setSourceTeam(null);
            projectMemberRepository.save(member);
            return;
        }

        // Aynı projeye bağlı başka bir takımda hâlâ üyeyse erişimi korunur,
        // üyelik yalnızca o takımın sorumluluğuna geçer.
        List<ProjectTeam> covering = projectTeamRepository
                .findOtherLinksCoveringMember(project.getId(), team.getId(), email);
        if (!covering.isEmpty()) {
            member.setSourceTeam(covering.get(0).getTeam());
            projectMemberRepository.save(member);
            return;
        }

        projectMemberRepository.delete(member);
        log.info("Takım senkronu: {} projeden çıkarıldı ({} takımından ayrıldı)", email, team.getTeamName());
    }
}
