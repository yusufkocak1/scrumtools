package com.scrumtools.service;

import com.scrumtools.dto.RoleRequest;
import com.scrumtools.dto.RoleResponse;
import com.scrumtools.entity.Role;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.RoleScope;
import com.scrumtools.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<RoleResponse> getRolesByScope(RoleScope scope) {
        return roleRepository.findByScope(scope).stream().map(this::toResponse).toList();
    }

    public List<RoleResponse> getDefaultRoles() {
        return roleRepository.findByIsDefaultTrue().stream().map(this::toResponse).toList();
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .scope(request.scope())
                .scopeId(request.scopeId())
                .permissions(request.permissions() != null ? request.permissions() : Set.of())
                .color(request.color() != null ? request.color() : "#6B7280")
                .isDefault(false)
                .build();
        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID roleId, RoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol bulunamadı: " + roleId));

        if (role.getIsDefault()) {
            throw new IllegalArgumentException("Varsayılan roller güncellenemez.");
        }

        role.setName(request.name());
        role.setDescription(request.description());
        if (request.permissions() != null) role.setPermissions(request.permissions());
        if (request.color() != null) role.setColor(request.color());

        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol bulunamadı: " + roleId));
        if (role.getIsDefault()) {
            throw new IllegalArgumentException("Varsayılan roller silinemez.");
        }
        roleRepository.delete(role);
    }

    public List<Permission> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }

    // Varsayılan rolleri seed etmek için kullanılır
    @Transactional
    public void seedDefaultRoles() {
        if (roleRepository.findByIsDefaultTrue().isEmpty()) {
            createDefaultRole("Super Admin", RoleScope.SYSTEM, EnumSet.allOf(Permission.class));
            createDefaultRole("Project Admin", RoleScope.PROJECT,
                    EnumSet.of(Permission.PROJECT_EDIT, Permission.PROJECT_MANAGE_SETTINGS,
                            Permission.TEAM_MANAGE_MEMBERS, Permission.TEAM_MANAGE_ROLES,
                            Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_ALL, Permission.ISSUE_DELETE,
                            Permission.ISSUE_ASSIGN, Permission.ISSUE_CHANGE_STATUS, Permission.ISSUE_CHANGE_PRIORITY,
                            Permission.ISSUE_MANAGE_LABELS, Permission.ISSUE_MANAGE_SUBTASKS, Permission.ISSUE_LINK, Permission.ISSUE_MOVE,
                            Permission.SPRINT_CREATE, Permission.SPRINT_START, Permission.SPRINT_COMPLETE, Permission.SPRINT_EDIT, Permission.SPRINT_DELETE,
                            Permission.BOARD_CREATE, Permission.BOARD_EDIT, Permission.BOARD_DELETE,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_ALL, Permission.COMMENT_DELETE_ALL,
                            Permission.ATTACHMENT_UPLOAD, Permission.ATTACHMENT_DELETE_ALL,
                            Permission.REPORT_VIEW, Permission.REPORT_EXPORT, Permission.WORKFLOW_MANAGE));
            createDefaultRole("Scrum Master", RoleScope.PROJECT,
                    EnumSet.of(Permission.SPRINT_CREATE, Permission.SPRINT_START, Permission.SPRINT_COMPLETE,
                            Permission.SPRINT_EDIT, Permission.BOARD_CREATE, Permission.BOARD_EDIT,
                            Permission.ISSUE_MOVE, Permission.ISSUE_CHANGE_STATUS, Permission.ISSUE_ASSIGN,
                            Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_ALL,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_OWN, Permission.COMMENT_DELETE_OWN,
                            Permission.ATTACHMENT_UPLOAD, Permission.REPORT_VIEW));
            createDefaultRole("Product Owner", RoleScope.PROJECT,
                    EnumSet.of(Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_ALL, Permission.ISSUE_ASSIGN,
                            Permission.ISSUE_CHANGE_PRIORITY, Permission.ISSUE_MANAGE_LABELS,
                            Permission.ISSUE_CHANGE_STATUS, Permission.ISSUE_LINK, Permission.ISSUE_MOVE,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_OWN, Permission.COMMENT_DELETE_OWN,
                            Permission.ATTACHMENT_UPLOAD, Permission.REPORT_VIEW, Permission.REPORT_EXPORT));
            createDefaultRole("Developer", RoleScope.PROJECT,
                    EnumSet.of(Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_OWN, Permission.ISSUE_CHANGE_STATUS,
                            Permission.ISSUE_MANAGE_SUBTASKS,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_OWN, Permission.COMMENT_DELETE_OWN,
                            Permission.ATTACHMENT_UPLOAD, Permission.ATTACHMENT_DELETE_OWN,
                            Permission.REPORT_VIEW));
            createDefaultRole("QA / Tester", RoleScope.PROJECT,
                    EnumSet.of(Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_OWN, Permission.ISSUE_CHANGE_STATUS,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_OWN, Permission.COMMENT_DELETE_OWN,
                            Permission.ATTACHMENT_UPLOAD, Permission.REPORT_VIEW));
            createDefaultRole("Analyst", RoleScope.PROJECT,
                    EnumSet.of(Permission.ISSUE_CREATE, Permission.ISSUE_EDIT_OWN, Permission.ISSUE_MANAGE_LABELS,
                            Permission.COMMENT_CREATE, Permission.COMMENT_EDIT_OWN, Permission.COMMENT_DELETE_OWN,
                            Permission.ATTACHMENT_UPLOAD, Permission.REPORT_VIEW, Permission.REPORT_EXPORT));
            createDefaultRole("Viewer / Observer", RoleScope.PROJECT,
                    EnumSet.of(Permission.REPORT_VIEW));
        }
    }

    private void createDefaultRole(String name, RoleScope scope, Set<Permission> permissions) {
        Role role = Role.builder()
                .name(name)
                .scope(scope)
                .permissions(permissions)
                .isDefault(true)
                .color("#6B7280")
                .build();
        roleRepository.save(role);
    }

    private RoleResponse toResponse(Role r) {
        return new RoleResponse(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getScope(),
                r.getScopeId(),
                r.getIsDefault(),
                r.getPermissions(),
                r.getColor(),
                r.getCreatedAt()
        );
    }
}

