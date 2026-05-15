package com.scrumtools.security;

import com.scrumtools.entity.enums.Permission;
import com.scrumtools.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @PreAuthorize("@projectSecurity.hasPermission(authentication, #projectId, 'ISSUE_CREATE')")
 * şeklinde kullanılır.
 */
@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurityEvaluator {

    private final PermissionService permissionService;

    public boolean hasPermission(Authentication authentication, UUID projectId, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        String email = authentication.getName();
        try {
            Permission permission = Permission.valueOf(permissionName);
            return permissionService.hasProjectPermission(email, projectId, permission);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return permissionService.hasSystemRole(authentication.getName(),
                com.scrumtools.entity.enums.SystemRole.SUPER_ADMIN);
    }
}

