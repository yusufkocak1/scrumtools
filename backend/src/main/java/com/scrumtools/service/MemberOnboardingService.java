package com.scrumtools.service;

import com.scrumtools.dto.CreateMemberRequest;
import com.scrumtools.dto.InviteResponse;
import com.scrumtools.dto.OrgMemberResponse;
import com.scrumtools.entity.EmailMessage;
import com.scrumtools.entity.Invitation;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.OrganizationMember;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.InvitationStatus;
import com.scrumtools.entity.enums.InvitationType;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.TokenPurpose;
import com.scrumtools.repository.EmailMessageRepository;
import com.scrumtools.repository.InvitationRepository;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.mail.MailService;
import com.scrumtools.service.mail.PostForgeMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final EmailMessageRepository emailMessageRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordTokenService passwordTokenService;
    private final MailService mailService;
    private final EntitlementService entitlementService;

    /**
     * Organizasyonun GÖNDERDİĞİ davetler (en yeni önce). Kaynak {@code invitations}
     * tablosudur — hem mail giden (hesabı olmayan davetli) hem de uygulama içi
     * davetler burada toplanır.
     * <p>
     * Mail durumu {@code email_messages}'tan e-posta adresiyle eşlenir; aynı adrese
     * birden çok davet gönderilmişse en yeni mail kaydı kullanılır.
     */
    @Transactional(readOnly = true)
    public List<InviteResponse> listInvites(UUID orgId, String requesterEmail) {
        checkAdminAccess(orgId, requesterEmail);

        List<Invitation> invitations = invitationRepository
                .findByTargetIdAndTypeOrderByCreatedAtDesc(orgId, InvitationType.ORGANIZATION);
        if (invitations.isEmpty()) return List.of();

        // Liste zaten en yeni önce — çakışmada ilk (en yeni) kayıt korunur
        Map<String, EmailMessage> mailByEmail = emailMessageRepository
                .findByOrganizationIdAndTemplateCodeOrderByCreatedAtDesc(
                        orgId, PostForgeMailService.T_MEMBER_INVITE)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getRecipient().toLowerCase(),
                        m -> m,
                        (newest, older) -> newest));

        Set<String> emails = invitations.stream()
                .map(i -> i.getEmail().toLowerCase())
                .collect(Collectors.toSet());
        Map<String, User> userByEmail = userRepository.findByEmailIn(emails).stream()
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u, (a, b) -> a));

        return invitations.stream()
                .map(inv -> toInviteResponse(
                        inv,
                        mailByEmail.get(inv.getEmail().toLowerCase()),
                        userByEmail.get(inv.getEmail().toLowerCase())))
                .toList();
    }

    private InviteResponse toInviteResponse(Invitation inv, EmailMessage mail, User invitee) {
        return new InviteResponse(
                inv.getId(),
                inv.getEmail(),
                invitee != null ? invitee.getName() : null,
                effectiveStatus(inv),
                inv.getInvitedBy() != null ? inv.getInvitedBy().getName() : null,
                inv.getCreatedAt(),
                inv.getExpiresAt(),
                mail != null ? mail.getStatus() : null,
                mail != null ? mail.getSentAt() : null,
                mail != null ? mail.getOpenedAt() : null,
                mail != null ? mail.getFirstClickedAt() : null,
                mail != null ? mail.getClickCount() : 0,
                mail != null ? mail.getFailureReason() : null,
                invitee != null && Boolean.TRUE.equals(invitee.getEmailVerified())
        );
    }

    /**
     * Süresi geçmiş PENDING davetler DB'de PENDING kalır (durum yalnızca erişim
     * denemesinde güncellenir) — listede doğru görünmesi için burada hesaplanır.
     */
    private InvitationStatus effectiveStatus(Invitation inv) {
        if (inv.getStatus() == InvitationStatus.PENDING
                && inv.getExpiresAt() != null
                && inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            return InvitationStatus.EXPIRED;
        }
        return inv.getStatus();
    }

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
            mailService.sendMemberInvite(target, org, passwordTokenService.setupUrl(rawToken));
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
