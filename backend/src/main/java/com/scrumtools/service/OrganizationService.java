package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.entity.enums.SubscriptionStatus;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final EntitlementService entitlementService;
    private final TeamService teamService;
    private final StorageService storageService;

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2MB

    /**
     * SVG kasıtlı olarak dışarıda: dosyalar uygulamayla aynı origin üzerinden
     * servis edildiğinden, script içeren bir SVG doğrudan açıldığında oturum
     * verisine erişebilirdi.
     */
    private static final Set<String> ALLOWED_LOGO_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    /** Logo presigned URL ömrü — SPA oturumu boyunca kırılmaması için uzun tutulur (MinIO üst sınırı 7 gün). */
    private static final int LOGO_URL_EXPIRY_MINUTES = 7 * 24 * 60;

    @Transactional
    public OrganizationResponse createOrganization(String userEmail, OrganizationRequest request) {
        User user = getUserByEmail(userEmail);

        if (organizationRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Bu slug zaten kullanımda: " + request.slug());
        }

        Organization org = Organization.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .owner(user)
                .build();
        org = organizationRepository.save(org);

        // Sahibi otomatik olarak ORG_OWNER olarak ekle
        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(user)
                .orgRole(OrgRole.ORG_OWNER)
                .build();
        organizationMemberRepository.save(member);

        startTrialIfEligible(org, user);

        return toResponse(org, 1, OrgRole.ORG_OWNER);
    }

    /**
     * Yeni organizasyona üst paket trial'ı başlatır.
     * Suistimal önlemi: her kullanıcı, sahibi olduğu org'lar için yalnızca BİR kez
     * trial başlatabilir; sonraki org'ları FREE olarak başlar.
     */
    private void startTrialIfEligible(Organization org, User owner) {
        if (subscriptionRepository.existsByOrganizationOwnerIdAndSource(owner.getId(), SubscriptionSource.TRIAL)) {
            return; // org.plan varsayılanı zaten FREE
        }
        Plan trialPlan = planService.getTrialPlan();
        LocalDateTime now = LocalDateTime.now();
        subscriptionRepository.save(Subscription.builder()
                .organization(org)
                .plan(trialPlan)
                .status(SubscriptionStatus.TRIAL)
                .source(SubscriptionSource.TRIAL)
                .currentPeriodStart(now)
                .trialEndsAt(now.plusDays(trialPlan.getTrialDays()))
                .build());
        org.setPlan(trialPlan.getCode());
        organizationRepository.save(org);
        log.info("Org '{}' için {} günlük {} trial başlatıldı.", org.getSlug(), trialPlan.getTrialDays(), trialPlan.getCode());
    }

    public List<OrganizationResponse> getMyOrganizations(String userEmail) {
        return organizationRepository.findAllByMemberEmail(userEmail).stream()
                .map(org -> {
                    int count = organizationMemberRepository.findByOrganizationId(org.getId()).size();
                    return toResponse(org, count, roleOf(org.getId(), userEmail));
                })
                .toList();
    }

    public OrganizationResponse getOrganization(UUID orgId, String userEmail) {
        Organization org = getOrgById(orgId);
        checkMembership(orgId, userEmail);
        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count, roleOf(orgId, userEmail));
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID orgId, String userEmail, OrganizationRequest request) {
        Organization org = getOrgById(orgId);
        checkAdminAccess(orgId, userEmail);

        org.setName(request.name());
        org.setDescription(request.description());
        // Yüklenmiş logo varsa dış adres yazılmaz — yanıttaki presigned URL'in
        // geri gönderilmesi logoyu bozmamalı.
        if (request.logoUrl() != null && org.getLogoObjectKey() == null) {
            org.setLogoUrl(request.logoUrl());
        }
        org = organizationRepository.save(org);

        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count, roleOf(orgId, userEmail));
    }

    /**
     * Organizasyon logosunu dosya olarak yükler (MinIO). Önceki logo dosyası silinir —
     * her org için tek bir logo tutulur, kullanılmayan nesneler bucket'ta birikmez.
     */
    @Transactional
    public OrganizationResponse uploadLogo(UUID orgId, String userEmail, MultipartFile file) {
        Organization org = getOrgById(orgId);
        checkAdminAccess(orgId, userEmail);
        validateLogo(file);

        String previousKey = org.getLogoObjectKey();
        String objectKey = storageService.upload(String.format("organizations/%s/logo", orgId), file);

        org.setLogoObjectKey(objectKey);
        // Dış adres artık geçersiz: kaynak tek olmalı, yoksa hangisinin gösterildiği belirsizleşir.
        org.setLogoUrl(null);
        org = organizationRepository.save(org);

        deleteQuietly(previousKey);
        log.info("Org '{}' logosu güncellendi: {}", org.getSlug(), objectKey);

        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count, roleOf(orgId, userEmail));
    }

    /** Logoyu kaldırır — yüklenmiş dosya da dış adres de temizlenir, baş harf rozetine dönülür. */
    @Transactional
    public OrganizationResponse deleteLogo(UUID orgId, String userEmail) {
        Organization org = getOrgById(orgId);
        checkAdminAccess(orgId, userEmail);

        String previousKey = org.getLogoObjectKey();
        org.setLogoObjectKey(null);
        org.setLogoUrl(null);
        org = organizationRepository.save(org);

        deleteQuietly(previousKey);

        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count, roleOf(orgId, userEmail));
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo dosyası boş olamaz.");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("Logo boyutu 2MB'ı aşamaz.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_LOGO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Logo yalnızca PNG, JPEG, WEBP veya GIF olabilir.");
        }
    }

    /**
     * Eski dosyanın silinememesi kullanıcı akışını bozmamalı — yeni logo zaten
     * kaydedildi, artık kullanılmayan nesne yalnızca depolamada yer tutar.
     */
    private void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            storageService.delete(objectKey);
        } catch (Exception e) {
            log.warn("Eski logo silinemedi ({}): {}", objectKey, e.getMessage());
        }
    }

    /** Org üyesinin görebileceği efektif paket hakları (plan kartları/limit göstergeleri için). */
    public EntitlementsResponse getEntitlements(UUID orgId, String userEmail) {
        checkMembership(orgId, userEmail);
        return entitlementService.getEntitlements(orgId);
    }

    public List<OrgMemberResponse> getMembers(UUID orgId, String userEmail) {
        checkMembership(orgId, userEmail);
        return organizationMemberRepository.findByOrganizationId(orgId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public OrgMemberResponse addMember(UUID orgId, String requesterEmail, String targetEmail, OrgRole role) {
        Organization org = getOrgById(orgId);
        checkAdminAccess(orgId, requesterEmail);
        entitlementService.assertCanAddMember(orgId);

        User targetUser = getUserByEmail(targetEmail);
        User requester = getUserByEmail(requesterEmail);

        if (organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, targetUser.getId())) {
            throw new IllegalArgumentException("Kullanıcı zaten bu organizasyonun üyesi.");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(targetUser)
                .orgRole(role != null ? role : OrgRole.ORG_MEMBER)
                .invitedBy(requester)
                .build();
        member = organizationMemberRepository.save(member);
        return toMemberResponse(member);
    }

    @Transactional
    public OrgMemberResponse updateMemberRole(UUID orgId, UUID userId, String requesterEmail, OrgRole newRole) {
        checkOwnerAccess(orgId, requesterEmail);
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı."));
        member.setOrgRole(newRole);
        member = organizationMemberRepository.save(member);
        return toMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId, String requesterEmail) {
        checkAdminAccess(orgId, requesterEmail);
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı."));

        if (member.getOrgRole() == OrgRole.ORG_OWNER) {
            throw new IllegalArgumentException("Organizasyon sahibi organizasyondan çıkarılamaz.");
        }

        // Takım ve takım-bağlı proje üyelikleri de temizlenir — org'dan çıkan kişi
        // hiçbir takım/proje ekranında görünmeye devam etmemeli.
        teamService.removeUserFromOrgTeams(orgId, member.getUser().getEmail());
        organizationMemberRepository.delete(member);
    }

    // --- Helpers ---

    private Organization getOrgById(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    private void checkMembership(UUID orgId, String email) {
        Organization org = getOrgById(orgId);
        if (org.isSuspendedOrg()) {
            throw new SecurityException("Bu organizasyon askıya alınmış durumda. Lütfen destek ile iletişime geçin.");
        }
        User user = getUserByEmail(email);
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, user.getId())
                .orElseThrow(() -> new SecurityException("Bu organizasyona erişim yetkiniz yok."));
        if (!member.isActiveMember()) {
            throw new SecurityException(
                    "Üyeliğiniz paket limiti nedeniyle pasif durumda. Organizasyon sahibinizle iletişime geçin.");
        }
    }

    private void checkAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Bu işlem için yeterli yetkiniz yok.");
        }
    }

    private void checkOwnerAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER))) {
            throw new SecurityException("Bu işlem sadece organizasyon sahibi tarafından yapılabilir.");
        }
    }

    private OrganizationResponse toResponse(Organization org, int memberCount, OrgRole myRole) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDescription(),
                resolveLogoUrl(org),
                org.getOwner().getId(),
                org.getOwner().getName(),
                org.getPlan(),
                org.getMaxMembers(),
                memberCount,
                org.getCreatedAt(),
                myRole
        );
    }

    /**
     * Görüntülenecek logo adresi: yüklenmiş dosya varsa presigned URL, yoksa
     * kayıtlı dış adres. Depolama erişilemezse logo boş döner — organizasyon
     * listesi kritik yoldur, logo yüzünden başarısız olmamalı.
     */
    private String resolveLogoUrl(Organization org) {
        if (org.getLogoObjectKey() == null || org.getLogoObjectKey().isBlank()) {
            return org.getLogoUrl();
        }
        try {
            return storageService.getPresignedUrl(org.getLogoObjectKey(), LOGO_URL_EXPIRY_MINUTES);
        } catch (Exception e) {
            log.warn("Org '{}' logo URL'i üretilemedi: {}", org.getSlug(), e.getMessage());
            return null;
        }
    }

    /** İsteği yapan kullanıcının org rolü — arayüzün menüleri gizleyebilmesi için. */
    private OrgRole roleOf(UUID orgId, String email) {
        return organizationMemberRepository.findByOrganizationIdAndUserEmail(orgId, email)
                .map(OrganizationMember::getOrgRole)
                .orElse(null);
    }

    private OrgMemberResponse toMemberResponse(OrganizationMember m) {
        return new OrgMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                m.getUser().getAvatarUrl(),
                m.getOrgRole(),
                m.getJoinedAt()
        );
    }
}

