package com.scrumtools.service;

import com.scrumtools.dto.InvitationRequest;
import com.scrumtools.dto.InvitationResponse;
import com.scrumtools.entity.Invitation;
import com.scrumtools.entity.Role;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.InvitationStatus;
import com.scrumtools.entity.enums.InvitationType;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final EntitlementService entitlementService;
    private final MemberOnboardingService memberOnboardingService;

    @Transactional
    public InvitationResponse sendInvitation(String inviterEmail, InvitationRequest request) {
        User inviter = getUserByEmail(inviterEmail);

        // Daha önce bekleyen davet var mı?
        if (invitationRepository.existsByEmailAndTargetIdAndTypeAndStatus(
                request.email(), request.targetId(), request.type(), InvitationStatus.PENDING)) {
            throw new IllegalArgumentException("Bu kullanıcıya zaten bekleyen bir davet gönderilmiş.");
        }

        // Org daveti: paket üye limiti dolu ise davet daha gönderilirken engelle
        if (request.type() == InvitationType.ORGANIZATION) {
            entitlementService.assertCanAddMember(request.targetId());

            // Hesabı olmayan kullanıcı in-app daveti göremez — hesabını oluşturup
            // şifre-kurulum e-postası gönderen create-member akışına delege et.
            String targetEmail = request.email().toLowerCase().trim();
            if (!userRepository.existsByEmail(targetEmail)) {
                memberOnboardingService.createMember(request.targetId(), inviterEmail,
                        new com.scrumtools.dto.CreateMemberRequest(
                                targetEmail,
                                targetEmail.split("@")[0], // geçici görünen ad — üye ilk girişte güncelleyebilir
                                OrgRole.ORG_MEMBER));
                // Denetim izi için kabul edilmiş bir davet kaydı bırak
                Invitation autoAccepted = invitationRepository.save(Invitation.builder()
                        .email(targetEmail)
                        .type(request.type())
                        .targetId(request.targetId())
                        .token(generateToken())
                        .status(InvitationStatus.ACCEPTED)
                        .invitedBy(inviter)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .acceptedAt(LocalDateTime.now())
                        .build());
                log.info("Hesabı olmayan davet create-member'a delege edildi: {} -> {}", inviterEmail, targetEmail);
                return toResponse(autoAccepted);
            }
        }

        Role role = request.roleId() != null ? roleRepository.findById(request.roleId()).orElse(null) : null;

        Invitation invitation = Invitation.builder()
                .email(request.email())
                .type(request.type())
                .targetId(request.targetId())
                .role(role)
                .token(generateToken())
                .status(InvitationStatus.PENDING)
                .invitedBy(inviter)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Davet gönderildi: {} -> {} ({})", inviterEmail, request.email(), request.type());

        return toResponse(invitation);
    }

    public List<InvitationResponse> getPendingInvitations(String email) {
        return invitationRepository.findByEmailAndStatus(email, InvitationStatus.PENDING).stream()
                .filter(inv -> inv.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvitationResponse acceptInvitation(String token, String userEmail) {
        Invitation invitation = getValidInvitation(token);

        if (!invitation.getEmail().equalsIgnoreCase(userEmail)) {
            throw new SecurityException("Bu davet size ait değil.");
        }

        User user = getUserByEmail(userEmail);

        // Hedef entity'ye kullanıcıyı ekle
        processAcceptance(invitation, user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        return toResponse(invitation);
    }

    @Transactional
    public InvitationResponse declineInvitation(String token, String userEmail) {
        Invitation invitation = getValidInvitation(token);

        if (!invitation.getEmail().equalsIgnoreCase(userEmail)) {
            throw new SecurityException("Bu davet size ait değil.");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation = invitationRepository.save(invitation);
        return toResponse(invitation);
    }

    private void processAcceptance(Invitation invitation, User user) {
        if (invitation.getType() == InvitationType.ORGANIZATION) {
            if (!organizationMemberRepository.existsByOrganizationIdAndUserId(invitation.getTargetId(), user.getId())) {
                // Davet gönderildikten sonra limit dolmuş olabilir — kabul anında tekrar kontrol et
                entitlementService.assertCanAddMember(invitation.getTargetId());
                var org = organizationRepository.findById(invitation.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı."));
                var member = com.scrumtools.entity.OrganizationMember.builder()
                        .organization(org)
                        .user(user)
                        .orgRole(OrgRole.ORG_MEMBER)
                        .invitedBy(invitation.getInvitedBy())
                        .build();
                organizationMemberRepository.save(member);
            }
        } else if (invitation.getType() == InvitationType.PROJECT) {
            if (!projectMemberRepository.existsByProjectIdAndUserId(invitation.getTargetId(), user.getId())) {
                var project = projectRepository.findById(invitation.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı."));
                var member = com.scrumtools.entity.ProjectMember.builder()
                        .project(project)
                        .user(user)
                        .roles(invitation.getRole() != null
                                ? new java.util.HashSet<>(java.util.Set.of(invitation.getRole()))
                                : new java.util.HashSet<>())
                        .addedBy(invitation.getInvitedBy())
                        .build();
                projectMemberRepository.save(member);
            }
        }
    }

    private Invitation getValidInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz davet token'ı."));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Bu davet artık geçerli değil (durum: " + invitation.getStatus() + ").");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Bu davetiyenin süresi dolmuş.");
        }

        return invitation;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
    }

    private InvitationResponse toResponse(Invitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getType(),
                inv.getTargetId(),
                inv.getRole() != null ? inv.getRole().getId() : null,
                inv.getRole() != null ? inv.getRole().getName() : null,
                inv.getToken(),
                inv.getStatus(),
                inv.getInvitedBy().getName(),
                inv.getInvitedBy().getEmail(),
                inv.getExpiresAt(),
                inv.getAcceptedAt(),
                inv.getCreatedAt()
        );
    }
}

