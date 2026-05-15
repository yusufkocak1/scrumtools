package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

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

        return toResponse(org, 1);
    }

    public List<OrganizationResponse> getMyOrganizations(String userEmail) {
        return organizationRepository.findAllByMemberEmail(userEmail).stream()
                .map(org -> {
                    int count = organizationMemberRepository.findByOrganizationId(org.getId()).size();
                    return toResponse(org, count);
                })
                .toList();
    }

    public OrganizationResponse getOrganization(UUID orgId, String userEmail) {
        Organization org = getOrgById(orgId);
        checkMembership(orgId, userEmail);
        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID orgId, String userEmail, OrganizationRequest request) {
        Organization org = getOrgById(orgId);
        checkAdminAccess(orgId, userEmail);

        org.setName(request.name());
        org.setDescription(request.description());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        org = organizationRepository.save(org);

        int count = organizationMemberRepository.findByOrganizationId(orgId).size();
        return toResponse(org, count);
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
        User user = getUserByEmail(email);
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new SecurityException("Bu organizasyona erişim yetkiniz yok.");
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

    private OrganizationResponse toResponse(Organization org, int memberCount) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDescription(),
                org.getLogoUrl(),
                org.getOwner().getId(),
                org.getOwner().getName(),
                org.getPlan(),
                org.getMaxMembers(),
                memberCount,
                org.getCreatedAt()
        );
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

