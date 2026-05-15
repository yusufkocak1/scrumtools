package com.scrumtools.service;

import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    /**
     * Kullanıcının belirli bir projede belirli bir izne sahip olup olmadığını kontrol eder.
     */
    public boolean hasProjectPermission(String userEmail, UUID projectId, Permission permission) {
        // Super admin her şeye erişebilir
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + userEmail));

        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            return true;
        }

        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, userEmail)
                .map(member -> member.hasPermission(permission))
                .orElse(false);
    }

    /**
     * İzin yoksa exception fırlatır.
     */
    public void checkProjectPermission(String userEmail, UUID projectId, Permission permission) {
        if (!hasProjectPermission(userEmail, projectId, permission)) {
            throw new SecurityException("Bu işlem için '" + permission.name() + "' iznine ihtiyacınız var.");
        }
    }

    /**
     * Kullanıcının sistem rolünü kontrol eder.
     */
    public boolean hasSystemRole(String userEmail, SystemRole role) {
        return userRepository.findByEmail(userEmail)
                .map(u -> u.getSystemRole() == role || u.getSystemRole() == SystemRole.SUPER_ADMIN)
                .orElse(false);
    }
}

