package com.scrumtools.service;

import com.scrumtools.dto.AdminOrgBillingResponse;
import com.scrumtools.dto.AdminSubscriptionActionRequest;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.PaymentTransaction;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.PaymentStatus;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.PaymentTransactionRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Superadmin müşteri yönetimi: org listesi + abonelik aksiyonları + askıya alma.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBillingService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public List<AdminOrgBillingResponse> listOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::toRow)
                .toList();
    }

    @Transactional
    public AdminOrgBillingResponse applySubscriptionAction(UUID orgId, AdminSubscriptionActionRequest request,
                                                           String actorEmail) {
        User actor = getUser(actorEmail);
        String action = request.action().toUpperCase().trim();
        BillingCycle cycle = request.cycle() != null ? request.cycle() : BillingCycle.MONTHLY;

        switch (action) {
            case "ACTIVATE", "CHANGE_PLAN" -> {
                if (request.planCode() == null || request.planCode().isBlank()) {
                    throw new IllegalArgumentException("Bu aksiyon için planCode gerekli.");
                }
                subscriptionService.activate(orgId, request.planCode(), cycle,
                        SubscriptionSource.MANUAL, actor, request.periodEnd());
            }
            case "EXTEND" -> {
                Subscription live = subscriptionService.findLive(orgId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Uzatılacak canlı abonelik yok — önce ACTIVATE kullanın."));
                subscriptionService.activate(orgId, live.getPlan().getCode(), cycle,
                        SubscriptionSource.MANUAL, actor, request.periodEnd());
            }
            case "EXTEND_TRIAL" -> subscriptionService.extendTrial(orgId,
                    request.trialDays() != null ? request.trialDays() : 14, actor);
            case "CANCEL" -> subscriptionService.cancel(orgId, actor);
            default -> throw new IllegalArgumentException("Bilinmeyen aksiyon: " + action);
        }
        log.info("Superadmin abonelik aksiyonu: {} org={} actor={}", action, orgId, actorEmail);
        return toRow(getOrg(orgId));
    }

    @Transactional
    public AdminOrgBillingResponse setSuspended(UUID orgId, boolean suspended) {
        Organization org = getOrg(orgId);
        org.setSuspended(suspended);
        organizationRepository.save(org);
        log.info("Org askıya alma durumu değişti: {} suspended={}", org.getSlug(), suspended);
        return toRow(org);
    }

    private AdminOrgBillingResponse toRow(Organization org) {
        Optional<Subscription> live = subscriptionService.findLive(org.getId());
        Optional<PaymentTransaction> lastPayment = paymentTransactionRepository
                .findFirstByOrganizationIdAndStatusOrderByPaidAtDesc(org.getId(), PaymentStatus.PAID);

        return new AdminOrgBillingResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getOwner().getName(),
                org.getOwner().getEmail(),
                org.getPlan(),
                live.map(s -> s.getStatus().name()).orElse("FREE"),
                live.map(Subscription::getTrialEndsAt).orElse(null),
                live.map(Subscription::getCurrentPeriodEnd).orElse(null),
                organizationMemberRepository.countActiveByOrganizationId(org.getId()),
                org.isSuspendedOrg(),
                lastPayment.map(PaymentTransaction::getPaidAt).orElse(null),
                lastPayment.map(PaymentTransaction::getAmount).orElse(null),
                org.getCreatedAt()
        );
    }

    private Organization getOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }
}
