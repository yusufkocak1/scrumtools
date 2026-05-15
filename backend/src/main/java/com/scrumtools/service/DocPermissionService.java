package com.scrumtools.service;

import com.scrumtools.dto.DocPermissionRequest;
import com.scrumtools.dto.DocPermissionResponse;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.*;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocPermissionService {

    private final DocPermissionRepository permissionRepository;
    private final DocSpaceRepository spaceRepository;
    private final DocPageRepository pageRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    // ─── Yetki Kontrolleri ──────────────────────────────────────────────────────

    /**
     * Space yönetim yetkisi: Proje admini veya DOCS_MANAGE_SPACES izni olan kullanıcı.
     */
    public void checkSpaceManageAccess(UUID projectId, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return;

        var member = projectMemberRepository.findByProjectIdAndUserEmail(projectId, user.getEmail())
                .orElseThrow(() -> new SecurityException("Bu projeye erişiminiz yok"));

        if (member.hasPermission(Permission.DOCS_MANAGE_SPACES)) {
            return;
        }

        throw new SecurityException("Space yönetimi için DOCS_MANAGE_SPACES yetkisine ihtiyacınız var");
    }

    /**
     * Space'e herhangi bir erişim yetkisi var mı? (Space listesi filtreleme için)
     */
    public boolean hasSpaceAccess(DocSpace space, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return true;
        if (isProjectAdmin(space.getProject().getId(), user)) return true;
        return hasAccessLevel(space, null, user, DocAccessLevel.READ);
    }

    /**
     * Okuma yetkisi kontrolü (space veya page seviyesinde).
     */
    public void checkReadAccess(DocSpace space, DocPage page, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return;
        if (isProjectAdmin(space.getProject().getId(), user)) return;

        if (!hasAccessLevel(space, page, user, DocAccessLevel.READ)) {
            throw new SecurityException("Bu içeriği görüntüleme yetkiniz yok");
        }
    }

    /**
     * Yazma yetkisi kontrolü (space veya page seviyesinde).
     */
    public void checkWriteAccess(DocSpace space, DocPage page, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return;
        if (isProjectAdmin(space.getProject().getId(), user)) return;

        if (!hasAccessLevel(space, page, user, DocAccessLevel.WRITE)) {
            throw new SecurityException("Bu içeriği düzenleme yetkiniz yok");
        }
    }

    /**
     * Erişim seviyesi kontrolü. Hiyerarşi: page-level → space-level.
     * ADMIN > WRITE > READ (üst seviye alt seviyeyi kapsar).
     */
    private boolean hasAccessLevel(DocSpace space, DocPage page, User user, DocAccessLevel requiredLevel) {
        // 1. Page-level izin kontrolü
        if (page != null) {
            if (checkPermissionsForTarget(permissionRepository.findByPageId(page.getId()), user, requiredLevel)) {
                return true;
            }
        }

        // 2. Space-level izin kontrolü
        if (checkPermissionsForTarget(permissionRepository.findBySpaceId(space.getId()), user, requiredLevel)) {
            return true;
        }

        return false;
    }

    private boolean checkPermissionsForTarget(List<DocPermission> permissions, User user, DocAccessLevel requiredLevel) {
        for (DocPermission perm : permissions) {
            if (isPermissionApplicable(perm, user) && isLevelSufficient(perm.getAccessLevel(), requiredLevel)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPermissionApplicable(DocPermission perm, User user) {
        return switch (perm.getTargetType()) {
            case USER -> perm.getTargetId().equals(user.getId());
            case TEAM -> teamMemberRepository.existsByTeamIdAndUserId(perm.getTargetId(), user.getId());
            case ORGANIZATION -> organizationMemberRepository.existsByOrganizationIdAndUserId(perm.getTargetId(), user.getId());
            case PROJECT_MEMBERS -> projectMemberRepository.findByProjectIdAndUserEmail(perm.getTargetId(), user.getEmail()).isPresent();
        };
    }

    private boolean isLevelSufficient(DocAccessLevel granted, DocAccessLevel required) {
        return granted.ordinal() >= required.ordinal();
    }

    private boolean isProjectAdmin(UUID projectId, User user) {
        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, user.getEmail())
                .map(member -> member.hasPermission(Permission.DOCS_MANAGE_SPACES))
                .orElse(false);
    }

    // ─── Yetki Yönetimi (CRUD) ─────────────────────────────────────────────────

    @Transactional
    public DocPermissionResponse grantPermission(UUID projectId, DocPermissionRequest request, User grantedBy) {
        if (grantedBy.getSystemRole() != SystemRole.SUPER_ADMIN) {
            checkSpaceManageAccess(projectId, grantedBy);
        }

        DocSpace space = null;
        DocPage page = null;

        if (request.spaceId() != null) {
            space = spaceRepository.findById(request.spaceId())
                    .orElseThrow(() -> new RuntimeException("Space bulunamadı"));
        }
        if (request.pageId() != null) {
            page = pageRepository.findById(request.pageId())
                    .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));
        }

        if (space == null && page == null) {
            throw new IllegalArgumentException("Space veya Page belirtilmeli");
        }

        DocPermission permission = DocPermission.builder()
                .space(space)
                .page(page)
                .accessLevel(request.accessLevel())
                .targetType(request.targetType())
                .targetId(request.targetId())
                .grantedBy(grantedBy)
                .canDelegate(request.canDelegate())
                .build();

        permission = permissionRepository.save(permission);
        log.info("Doc permission verildi: {} {} → {} ({})",
                request.targetType(), request.targetId(), request.accessLevel(),
                space != null ? "space:" + space.getId() : "page:" + page.getId());

        String targetName = resolveTargetName(request.targetType(), request.targetId());
        return DocPermissionResponse.from(permission, targetName);
    }

    /**
     * Yetki devri: can_delegate=true olan kullanıcı kendi seviyesinde veya altında yetki verebilir.
     */
    @Transactional
    public DocPermissionResponse delegatePermission(UUID projectId, DocPermissionRequest request, User delegatingUser) {
        // Delege eden kullanıcının ilgili kaynak üzerinde can_delegate=true yetkisi olmalı
        UUID resourceSpaceId = request.spaceId();
        UUID resourcePageId = request.pageId();

        List<DocPermission> userPerms;
        if (resourcePageId != null) {
            userPerms = permissionRepository.findByPageAndTarget(resourcePageId, DocTargetType.USER, delegatingUser.getId());
        } else if (resourceSpaceId != null) {
            userPerms = permissionRepository.findBySpaceAndTarget(resourceSpaceId, DocTargetType.USER, delegatingUser.getId());
        } else {
            throw new IllegalArgumentException("Space veya Page belirtilmeli");
        }

        boolean canDelegate = userPerms.stream()
                .anyMatch(p -> p.getCanDelegate() && isLevelSufficient(p.getAccessLevel(), request.accessLevel()));

        if (!canDelegate && delegatingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            // Takım/org üyeliği üzerinden de kontrol et
            List<DocPermission> allPerms;
            if (resourcePageId != null) {
                allPerms = permissionRepository.findByPageId(resourcePageId);
            } else {
                allPerms = permissionRepository.findBySpaceId(resourceSpaceId);
            }

            canDelegate = allPerms.stream()
                    .anyMatch(p -> p.getCanDelegate() &&
                            isPermissionApplicable(p, delegatingUser) &&
                            isLevelSufficient(p.getAccessLevel(), request.accessLevel()));
        }

        if (!canDelegate) {
            throw new SecurityException("Bu yetkiyi devretme hakkınız yok. can_delegate izniniz olmalı.");
        }

        return grantPermission(projectId, request, delegatingUser);
    }

    public List<DocPermissionResponse> getPermissions(UUID spaceId, UUID pageId) {
        List<DocPermission> perms;
        if (pageId != null) {
            perms = permissionRepository.findByPageId(pageId);
        } else if (spaceId != null) {
            perms = permissionRepository.findBySpaceId(spaceId);
        } else {
            throw new IllegalArgumentException("Space veya Page belirtilmeli");
        }

        return perms.stream()
                .map(p -> DocPermissionResponse.from(p, resolveTargetName(p.getTargetType(), p.getTargetId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokePermission(UUID permissionId, User revokedBy) {
        DocPermission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission bulunamadı"));

        UUID projectId = perm.getSpace() != null
                ? perm.getSpace().getProject().getId()
                : perm.getPage().getSpace().getProject().getId();

        if (revokedBy.getSystemRole() != SystemRole.SUPER_ADMIN) {
            checkSpaceManageAccess(projectId, revokedBy);
        }

        permissionRepository.delete(perm);
        log.info("Doc permission kaldırıldı: {}", permissionId);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String resolveTargetName(DocTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId)
                    .map(User::getName)
                    .orElse("Bilinmeyen Kullanıcı");
            case TEAM -> "Takım";
            case ORGANIZATION -> "Organizasyon";
            case PROJECT_MEMBERS -> "Proje Üyeleri";
        };
    }
}

