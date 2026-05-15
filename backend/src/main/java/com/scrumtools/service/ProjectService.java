package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.MemberType;
import com.scrumtools.entity.enums.ProjectStatus;
import com.scrumtools.entity.enums.ProjectType;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public ProjectResponse createProject(UUID orgId, String userEmail, ProjectRequest request) {
        User user = getUserByEmail(userEmail);
        Organization org = getOrgById(orgId);

        // Organizasyon üyesi mi?
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new SecurityException("Bu organizasyona üye değilsiniz.");
        }

        if (projectRepository.existsByOrganizationIdAndKey(orgId, request.key())) {
            throw new IllegalArgumentException("Bu proje anahtarı organizasyonda zaten mevcut: " + request.key());
        }

        Project project = Project.builder()
                .organization(org)
                .name(request.name())
                .key(request.key())
                .description(request.description())
                .projectType(request.projectType() != null ? request.projectType() : ProjectType.SCRUM)
                .iconUrl(request.iconUrl())
                .color(request.color() != null ? request.color() : "#3B82F6")
                .lead(user)
                .build();
        project = projectRepository.save(project);

        // Proje oluşturucu otomatik olarak proje admini olarak ekle
        Role adminRole = roleRepository.findByNameAndScope("Project Admin", com.scrumtools.entity.enums.RoleScope.SYSTEM)
                .orElse(null);

        Set<Role> adminRoles = new java.util.HashSet<>();
        if (adminRole != null) adminRoles.add(adminRole);

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .roles(adminRoles)
                .addedBy(user)
                .build();
        projectMemberRepository.save(member);

        return toResponse(project, 1);
    }

    public List<ProjectResponse> getProjectsByOrg(UUID orgId, String userEmail) {
        // Organizasyon var mı kontrol et
        if (!organizationRepository.existsById(orgId)) {
            throw new IllegalArgumentException("Organizasyon bulunamadı: " + orgId);
        }
        User user = getUserByEmail(userEmail);

        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new SecurityException("Bu organizasyona üye değilsiniz.");
        }

        return projectRepository.findByOrganizationIdAndStatus(orgId, ProjectStatus.ACTIVE).stream()
                .map(p -> toResponse(p, projectMemberRepository.findByProjectId(p.getId()).size()))
                .toList();
    }

    public ProjectResponse getProject(UUID projectId, String userEmail) {
        Project project = getProjectById(projectId);
        checkProjectMembership(projectId, userEmail);
        int count = projectMemberRepository.findByProjectId(projectId).size();
        return toResponse(project, count);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, String userEmail, ProjectRequest request) {
        Project project = getProjectById(projectId);
        checkProjectAdmin(projectId, userEmail);

        project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        if (request.projectType() != null) project.setProjectType(request.projectType());
        if (request.iconUrl() != null) project.setIconUrl(request.iconUrl());
        if (request.color() != null) project.setColor(request.color());

        project = projectRepository.save(project);
        int count = projectMemberRepository.findByProjectId(projectId).size();
        return toResponse(project, count);
    }

    @Transactional
    public void deleteProject(UUID projectId, String userEmail) {
        Project project = getProjectById(projectId);
        checkProjectAdmin(projectId, userEmail);
        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);
    }

    public List<ProjectMemberResponse> getMembers(UUID projectId, String userEmail) {
        checkProjectMembership(projectId, userEmail);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, String requesterEmail, String targetEmail, List<UUID> roleIds, MemberType memberType) {
        Project project = getProjectById(projectId);
        checkProjectAdmin(projectId, requesterEmail);

        User targetUser = getUserByEmail(targetEmail);
        User requester = getUserByEmail(requesterEmail);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
            throw new IllegalArgumentException("Kullanıcı zaten bu projenin üyesi.");
        }

        Set<Role> roles = resolveRoles(roleIds);
        MemberType type = memberType != null ? memberType : MemberType.MEMBER;

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(targetUser)
                .roles(roles)
                .addedBy(requester)
                .memberType(type)
                .build();
        member = projectMemberRepository.save(member);
        return toMemberResponse(member);
    }

    @Transactional
    public List<ProjectMemberResponse> addTeamToProject(UUID projectId, UUID teamId, String requesterEmail, List<UUID> roleIds, MemberType memberType) {
        Project project = getProjectById(projectId);
        checkProjectAdmin(projectId, requesterEmail);
        User requester = getUserByEmail(requesterEmail);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        Set<Role> roles = resolveRoles(roleIds);
        MemberType type = memberType != null ? memberType : MemberType.MEMBER;

        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
        List<ProjectMemberResponse> added = new java.util.ArrayList<>();

        for (TeamMember tm : teamMembers) {
            userRepository.findByEmail(tm.getEmail()).ifPresent(targetUser -> {
                if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
                    ProjectMember pm = ProjectMember.builder()
                            .project(project)
                            .user(targetUser)
                            .roles(new java.util.HashSet<>(roles))
                            .addedBy(requester)
                            .memberType(type)
                            .build();
                    added.add(toMemberResponse(projectMemberRepository.save(pm)));
                }
            });
        }

        return added;
    }

    @Transactional
    public ProjectMemberResponse updateMemberRoles(UUID projectId, UUID userId, String requesterEmail, List<UUID> roleIds) {
        checkProjectAdmin(projectId, requesterEmail);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı."));

        Set<Role> roles = new java.util.HashSet<>();
        if (roleIds != null) {
            for (UUID roleId : roleIds) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }
        }
        member.setRoles(roles);
        member = projectMemberRepository.save(member);
        return toMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId, String requesterEmail) {
        checkProjectAdmin(projectId, requesterEmail);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı."));
        projectMemberRepository.delete(member);
    }

    // --- Helpers ---

    private Set<Role> resolveRoles(List<UUID> roleIds) {
        Set<Role> roles = new java.util.HashSet<>();
        if (roleIds != null) {
            for (UUID id : roleIds) {
                roleRepository.findById(id).ifPresent(roles::add);
            }
        }
        return roles;
    }

    private Project getProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));
    }

    private Organization getOrgById(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    private void checkProjectMembership(UUID projectId, String email) {
        if (!projectMemberRepository.existsByProjectIdAndUserEmail(projectId, email)) {
            throw new SecurityException("Bu projeye erişim yetkiniz yok.");
        }
    }

    private void checkProjectAdmin(UUID projectId, String email) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserEmail(projectId, email)
                .orElseThrow(() -> new SecurityException("Bu projeye erişim yetkiniz yok."));

        Project project = getProjectById(projectId);
        // Lead veya admin rolü olan üyeler işlem yapabilir
        boolean isLead = project.getLead() != null && project.getLead().getEmail().equals(email);
        boolean hasAdminRole = member.getRoles().stream().anyMatch(role ->
                role.getName().contains("Admin") ||
                role.getPermissions().contains(com.scrumtools.entity.enums.Permission.PROJECT_MANAGE_SETTINGS) ||
                role.getPermissions().contains(com.scrumtools.entity.enums.Permission.ADMIN_FULL_ACCESS));

        if (!isLead && !hasAdminRole) {
            throw new SecurityException("Bu işlem için yeterli yetkiniz yok.");
        }
    }

    private ProjectResponse toResponse(Project p, int memberCount) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getKey(),
                p.getDescription(),
                p.getOrganization() != null ? p.getOrganization().getId() : null,
                p.getOrganization() != null ? p.getOrganization().getName() : null,
                p.getLead() != null ? p.getLead().getId() : null,
                p.getLead() != null ? p.getLead().getName() : null,
                p.getProjectType(),
                p.getIconUrl(),
                p.getColor(),
                p.getStatus(),
                memberCount,
                p.getCreatedAt()
        );
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember m) {
        List<ProjectMemberResponse.RoleInfo> roleInfos = m.getRoles().stream()
                .map(r -> new ProjectMemberResponse.RoleInfo(r.getId(), r.getName(), r.getColor()))
                .toList();

        return new ProjectMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                m.getUser().getAvatarUrl(),
                roleInfos,
                m.getJoinedAt(),
                m.getMemberType()
        );
    }
}

