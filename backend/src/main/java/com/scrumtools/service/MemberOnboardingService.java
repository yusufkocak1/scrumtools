package com.scrumtools.service;

import com.scrumtools.dto.CreateMemberRequest;
import com.scrumtools.dto.OrgMemberResponse;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.OrganizationMember;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.TokenPurpose;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Org sahibinin/admininin üyeyi doğrudan sisteme kaydetmesi:
 * bireysel kullanıcı kendisi üye olmaz — hesabı burada oluşturulur,
 * e-postasına şifre-kurulum linki gider, ilk erişimde şifresini belirler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberOnboardingService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordTokenService passwordTokenService;
    private final MailService mailService;
    private final EntitlementService entitlementService;

    @Transactional
    public OrgMemberResponse createMember(UUID orgId, String requesterEmail, CreateMemberRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
        checkAdminAccess(orgId, requesterEmail);
        entitlementService.assertCanAddMember(orgId);

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + requesterEmail));

        String email = request.email().toLowerCase().trim();
        User target = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = target == null;

        if (isNewUser) {
            // Kullanılamaz rastgele şifre — üye ancak kurulum linkiyle şifre belirleyip girebilir
            target = userRepository.save(User.builder()
                    .email(email)
                    .name(request.name().trim())
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .build());
        }

        if (organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, target.getId())) {
            throw new IllegalArgumentException("Kullanıcı zaten bu organizasyonun üyesi.");
        }

        OrganizationMember member = organizationMemberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(target)
                .orgRole(request.orgRole() != null ? request.orgRole() : OrgRole.ORG_MEMBER)
                .invitedBy(requester)
                .build());

        if (isNewUser) {
            String rawToken = passwordTokenService.createToken(target, TokenPurpose.ACCOUNT_SETUP, requester);
            mailService.sendMemberInvite(target, org.getName(), passwordTokenService.setupUrl(rawToken));
            log.info("Yeni üye oluşturuldu ve davet maili gönderildi: {} → org '{}'", email, org.getSlug());
        }

        return toMemberResponse(member);
    }

    private void checkAdminAccess(UUID orgId, String email) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserEmailAndOrgRoleIn(orgId, email,
                List.of(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN))) {
            throw new SecurityException("Bu işlem için yeterli yetkiniz yok.");
        }
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
