package com.scrumtools.service;

import com.scrumtools.dto.DocPermissionRequest;
import com.scrumtools.dto.DocPermissionResponse;
import com.scrumtools.dto.DocPermissionTargetResponse;
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
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;

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
        if (!hasReadAccess(space, page, user)) {
            throw new SecurityException("Bu içeriği görüntüleme yetkiniz yok");
        }
    }

    /**
     * Yazma yetkisi kontrolü (space veya page seviyesinde).
     */
    public void checkWriteAccess(DocSpace space, DocPage page, User user) {
        if (!hasWriteAccess(space, page, user)) {
            throw new SecurityException("Bu içeriği düzenleme yetkiniz yok");
        }
    }

    /**
     * Boolean karşılıklar — ortak çalışma alanı bunları kullanır (COLLAB_WORKSPACE_PLAN.md Y1).
     *
     * <p>Bir ortak doküman bir {@code DocPage}'e bağlıysa yetki kararı buraya
     * devredilir; kararı istisna yakalayarak öğrenmek yerine doğrudan sorabilmek
     * için ayrıştırıldı. {@code check*} metotlarının davranışı değişmedi.
     */
    public boolean hasReadAccess(DocSpace space, DocPage page, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return true;
        if (isProjectAdmin(space.getProject().getId(), user)) return true;
        return hasAccessLevel(space, page, user, DocAccessLevel.READ);
    }

    public boolean hasWriteAccess(DocSpace space, DocPage page, User user) {
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return true;
        if (isProjectAdmin(space.getProject().getId(), user)) return true;
        return hasAccessLevel(space, page, user, DocAccessLevel.WRITE);
    }

    /**
     * Kullanıcı bu space'te <i>bir şey</i> görebiliyor mu — space seviyesinde ya
     * da içindeki herhangi bir sayfada.
     *
     * <p><b>Neden ayrı bir kontrol:</b> yetki modeli sayfa seviyesini destekliyor
     * ama space ve sayfa ağacı uçları yalnızca space seviyesine bakıyordu. Sonuç:
     * tek bir sayfaya çağrılan kullanıcı sayfanın kendisini okuyabiliyor, ama
     * sayfayı açan ekranın ihtiyaç duyduğu {@code getSpace} ve {@code getPageTree}
     * çağrıları 403 dönüyordu — yani "tek sayfa paylaş" pratikte hiç çalışmıyordu.
     */
    public boolean hasAnyAccessInSpace(DocSpace space, User user) {
        if (hasReadAccess(space, null, user)) return true;
        return checkPermissionsForTarget(
                permissionRepository.findByPageSpaceId(space.getId()), user, DocAccessLevel.READ);
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

    /**
     * Yetkinin uygulanacağı kaynak — ya bir space ya da bir sayfa.
     *
     * <p>Ayrı bir tip olmasının sebebi, kaynağın <b>istekteki projeye ait
     * olduğunun</b> tek bir yerde doğrulanması. Eskiden yetki kontrolü
     * {@code projectId} üzerinden yapılıyor ama kaynak yalnızca {@code findById}
     * ile çekiliyordu: A projesinde DOCS_MANAGE_SPACES yetkisi olan biri, gövdeye
     * B projesinin space kimliğini yazarak <b>başka bir projenin dokümanına</b>
     * yetki verebiliyordu.
     */
    private record PermissionTarget(DocSpace space, DocPage page) {
        DocSpace effectiveSpace() {
            return space != null ? space : page.getSpace();
        }

        String describe() {
            return space != null ? "space:" + space.getId() : "page:" + page.getId();
        }
    }

    private PermissionTarget resolveTarget(UUID projectId, UUID spaceId, UUID pageId) {
        if (pageId != null) {
            DocPage page = pageRepository.findById(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Sayfa bulunamadı"));
            requireProject(projectId, page.getSpace());
            return new PermissionTarget(null, page);
        }
        if (spaceId != null) {
            DocSpace space = spaceRepository.findById(spaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Space bulunamadı"));
            requireProject(projectId, space);
            return new PermissionTarget(space, null);
        }
        throw new IllegalArgumentException("Space veya Page belirtilmeli");
    }

    private void requireProject(UUID projectId, DocSpace space) {
        if (!space.getProject().getId().equals(projectId)) {
            // Kaynak var ama başka projenin; "yetkiniz yok" demek, kimliğin geçerli
            // olduğunu sızdırmamak için "bulunamadı"dan daha doğru bir cevap değil —
            // ikisi de bilgi vermiyor, bu daha açık.
            throw new SecurityException("Bu kaynak bu projeye ait değil");
        }
    }

    @Transactional
    public DocPermissionResponse grantPermission(UUID projectId, DocPermissionRequest request, User grantedBy) {
        PermissionTarget target = resolveTarget(projectId, request.spaceId(), request.pageId());
        if (grantedBy.getSystemRole() != SystemRole.SUPER_ADMIN) {
            checkSpaceManageAccess(projectId, grantedBy);
        }
        return upsert(target, request, grantedBy);
    }

    /**
     * Yetki devri: {@code can_delegate} yetkisi olan kullanıcı kendi seviyesinde
     * veya altında yetki verebilir.
     *
     * <p><b>Düzeltilen hata:</b> bu metot son adımda {@code grantPermission}'ı
     * çağırıyordu, o da DOCS_MANAGE_SPACES istiyordu. Yani devretme hakkı olan
     * ama space yöneticisi olmayan kullanıcı — özelliğin tek hedef kitlesi —
     * her denemede {@code SecurityException} alıyordu. Devir artık yazma işlemini
     * doğrudan yapıyor.
     */
    @Transactional
    public DocPermissionResponse delegatePermission(UUID projectId, DocPermissionRequest request, User delegatingUser) {
        PermissionTarget target = resolveTarget(projectId, request.spaceId(), request.pageId());

        boolean allowed = delegatingUser.getSystemRole() == SystemRole.SUPER_ADMIN
                || isProjectAdmin(projectId, delegatingUser)
                || canDelegateAt(target, delegatingUser, request.accessLevel());

        if (!allowed) {
            throw new SecurityException("Bu yetkiyi devretme hakkınız yok. can_delegate izniniz olmalı.");
        }
        return upsert(target, request, delegatingUser);
    }

    /**
     * Kullanıcının bu kaynakta istenen seviyeyi devretme hakkı var mı?
     *
     * <p>Sayfa hedefinde space seviyesindeki devir hakkı da sayılıyor: bir space'te
     * devretme hakkı olan kişinin o space'in <i>tek bir sayfasını</i> paylaşamaması
     * tutarsız olurdu.
     */
    private boolean canDelegateAt(PermissionTarget target, User user, DocAccessLevel level) {
        if (target.page() != null
                && hasDelegatablePermission(permissionRepository.findByPageId(target.page().getId()), user, level)) {
            return true;
        }
        UUID spaceId = target.effectiveSpace().getId();
        return hasDelegatablePermission(permissionRepository.findBySpaceId(spaceId), user, level);
    }

    private boolean hasDelegatablePermission(List<DocPermission> permissions, User user, DocAccessLevel level) {
        return permissions.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanDelegate())
                && isPermissionApplicable(p, user)
                && isLevelSufficient(p.getAccessLevel(), level));
    }

    /**
     * Aynı hedefe ikinci kez yetki verilirse yeni satır değil <b>güncelleme</b>.
     *
     * <p>Eskiden her "Paylaş" tıklaması yeni bir satır üretiyordu; listede aynı
     * kişi birden çok kez görünüyor ve seviyesini düşürmek için hepsinin tek tek
     * kaldırılması gerekiyordu — biri unutulduğunda eski (yüksek) yetki yürürlükte
     * kalıyordu, yani "yetkiyi kıstım" sanan kullanıcı yanılıyordu.
     */
    private DocPermissionResponse upsert(PermissionTarget target, DocPermissionRequest request, User actor) {
        List<DocPermission> existing = target.page() != null
                ? permissionRepository.findByPageAndTarget(
                        target.page().getId(), request.targetType(), request.targetId())
                : permissionRepository.findBySpaceAndTarget(
                        target.space().getId(), request.targetType(), request.targetId());

        DocPermission permission;
        if (existing.isEmpty()) {
            permission = DocPermission.builder()
                    .space(target.space())
                    .page(target.page())
                    .accessLevel(request.accessLevel())
                    .targetType(request.targetType())
                    .targetId(request.targetId())
                    .grantedBy(actor)
                    .canDelegate(request.canDelegate())
                    .build();
        } else {
            // Yinelenmiş eski kayıtlar varsa ilki güncellenip kalanlar temizleniyor.
            permission = existing.get(0);
            permission.setAccessLevel(request.accessLevel());
            permission.setCanDelegate(request.canDelegate());
            permission.setGrantedBy(actor);
            if (existing.size() > 1) {
                permissionRepository.deleteAll(existing.subList(1, existing.size()));
            }
        }

        permission = permissionRepository.save(permission);
        log.info("Doc permission verildi: {} {} → {} ({})",
                request.targetType(), request.targetId(), request.accessLevel(), target.describe());

        return DocPermissionResponse.from(
                permission, resolveTargetName(request.targetType(), request.targetId()));
    }

    /**
     * Kaynağın yetki listesi.
     *
     * <p><b>Düzeltilen hata:</b> burada <i>hiçbir</i> yetki kontrolü yoktu ve
     * uç nokta yalnızca "giriş yapmış olmak" istiyordu. Kimliği bilinen herhangi
     * bir space/sayfa için — başka organizasyonunki dâhil — kimin hangi seviyede
     * eriştiği listelenebiliyordu.
     */
    @Transactional(readOnly = true)
    public List<DocPermissionResponse> getPermissions(UUID projectId, UUID spaceId, UUID pageId, User user) {
        PermissionTarget target = resolveTarget(projectId, spaceId, pageId);
        checkReadAccess(target.effectiveSpace(), target.page(), user);

        List<DocPermission> perms = target.page() != null
                ? permissionRepository.findByPageId(target.page().getId())
                : permissionRepository.findBySpaceId(target.space().getId());

        return perms.stream()
                .map(p -> DocPermissionResponse.from(p, resolveTargetName(p.getTargetType(), p.getTargetId())))
                .collect(Collectors.toList());
    }

    /**
     * Yetkiyi kaldırır.
     *
     * <p>Space yöneticisi olmayan ama devir hakkıyla paylaşım yapabilen kullanıcı
     * <b>kendi verdiği</b> yetkiyi geri alabilmeli; aksi hâlde paylaşımı yapan
     * kişi onu geri çeviremez ve her düzeltme için yöneticiye gitmek gerekirdi.
     */
    @Transactional
    public void revokePermission(UUID permissionId, User revokedBy) {
        DocPermission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission bulunamadı"));

        PermissionTarget target = new PermissionTarget(perm.getSpace(), perm.getPage());
        UUID projectId = target.effectiveSpace().getProject().getId();

        boolean allowed = revokedBy.getSystemRole() == SystemRole.SUPER_ADMIN
                || isProjectAdmin(projectId, revokedBy)
                || perm.getGrantedBy().getId().equals(revokedBy.getId())
                || canDelegateAt(target, revokedBy, perm.getAccessLevel());

        if (!allowed) {
            throw new SecurityException("Bu yetkiyi kaldırma hakkınız yok");
        }

        permissionRepository.delete(perm);
        log.info("Doc permission kaldırıldı: {}", permissionId);
    }

    /**
     * Space silinirken yetkileri temizler — hem space hem içindeki sayfalar için.
     *
     * <p>{@code doc_permissions} satırları {@code doc_spaces}/{@code doc_pages}'e
     * yabancı anahtarla bağlı; silmeden önce boşaltılmazsa silme işlemi kısıt
     * ihlaliyle 500 dönüyordu. Depoda bu iki metot vardı ama <b>hiç
     * çağrılmıyordu</b>.
     */
    @Transactional
    public void removeAllForSpace(UUID spaceId) {
        permissionRepository.deleteAll(permissionRepository.findByPageSpaceId(spaceId));
        permissionRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void removeAllForPage(UUID pageId) {
        permissionRepository.deleteByPageId(pageId);
    }

    // ─── Hedef Arama ────────────────────────────────────────────────────────────

    /**
     * Yetki hedefi araması: kullanıcı/takım için proje organizasyonu içinde
     * isim veya e-posta ile arama yapar. UUID bilmeye gerek kalmaz.
     */
    @Transactional(readOnly = true)
    public List<DocPermissionTargetResponse> searchTargets(UUID projectId, DocTargetType targetType, String query, User user) {
        if (user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            projectMemberRepository.findByProjectIdAndUserEmail(projectId, user.getEmail())
                    .orElseThrow(() -> new SecurityException("Bu projeye erişiminiz yok"));
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı"));
        Organization org = project.getOrganization();
        String q = query == null ? "" : query.trim();

        return switch (targetType) {
            case USER -> searchUsers(project, org, q);
            case TEAM -> searchTeams(org, q);
            case ORGANIZATION -> {
                if (org == null || (!q.isEmpty() && !org.getName().toLowerCase().contains(q.toLowerCase()))) {
                    yield List.of();
                }
                yield List.of(new DocPermissionTargetResponse(
                        org.getId(), org.getName(), "Tüm organizasyon üyeleri", DocTargetType.ORGANIZATION));
            }
            case PROJECT_MEMBERS -> List.of(new DocPermissionTargetResponse(
                    project.getId(), project.getName(), "Tüm proje üyeleri", DocTargetType.PROJECT_MEMBERS));
        };
    }

    private List<DocPermissionTargetResponse> searchUsers(Project project, Organization org, String q) {
        List<User> users;
        if (org != null) {
            users = organizationMemberRepository.searchByOrganizationIdAndUserNameOrEmail(org.getId(), q).stream()
                    .map(OrganizationMember::getUser)
                    .toList();
        } else {
            users = projectMemberRepository.searchByProjectIdAndUserNameOrEmail(project.getId(), q).stream()
                    .map(ProjectMember::getUser)
                    .toList();
        }
        return users.stream()
                .limit(SEARCH_RESULT_LIMIT)
                .map(u -> new DocPermissionTargetResponse(u.getId(), u.getName(), u.getEmail(), DocTargetType.USER))
                .collect(Collectors.toList());
    }

    private List<DocPermissionTargetResponse> searchTeams(Organization org, String q) {
        if (org == null) return List.of();
        return teamRepository.findByOrganizationIdAndTeamNameContainingIgnoreCaseOrderByTeamName(org.getId(), q).stream()
                .limit(SEARCH_RESULT_LIMIT)
                .map(t -> new DocPermissionTargetResponse(t.getId(), t.getTeamName(), t.getTeamCode(), DocTargetType.TEAM))
                .collect(Collectors.toList());
    }

    private static final int SEARCH_RESULT_LIMIT = 20;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String resolveTargetName(DocTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId)
                    .map(User::getName)
                    .orElse("Bilinmeyen Kullanıcı");
            case TEAM -> teamRepository.findById(targetId)
                    .map(Team::getTeamName)
                    .orElse("Bilinmeyen Takım");
            case ORGANIZATION -> organizationRepository.findById(targetId)
                    .map(Organization::getName)
                    .orElse("Bilinmeyen Organizasyon");
            case PROJECT_MEMBERS -> projectRepository.findById(targetId)
                    .map(p -> p.getName() + " üyeleri")
                    .orElse("Proje Üyeleri");
        };
    }
}

