package com.scrumtools.service;

import com.scrumtools.dto.EntitlementsResponse;
import com.scrumtools.entity.Organization;
import com.scrumtools.entity.Plan;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.ProjectStatus;
import com.scrumtools.entity.enums.SubscriptionStatus;
import com.scrumtools.exception.PlanLimitExceededException;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organizasyonun o anki efektif paket haklarını çözen tek merkez.
 *
 * Çözümleme sırası:
 *   1. Canlı abonelik (TRIAL / ACTIVE / PAST_DUE) varsa → onun planı
 *   2. Yoksa → varsayılan (FREE) plan
 *   3. Organization.maxMembers doluysa → plan limitini override eder (özel anlaşma)
 *
 * Plan/limit bilgisi JWT'ye veya localStorage'a GÖMÜLMEZ — her zaman buradan taze çözülür.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    /** Entitlement veren abonelik durumları (PAST_DUE = grace period, haklar devam eder). */
    public static final List<SubscriptionStatus> LIVE_STATUSES =
            List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final PlanService planService;

    public EntitlementsResponse getEntitlements(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));

        Optional<Subscription> live = findLiveSubscription(orgId);
        Plan plan = live.map(Subscription::getPlan).orElseGet(planService::getDefaultPlan);
        String status = live.map(s -> s.getStatus().name()).orElse("FREE");

        Integer maxMembers = org.getMaxMembers() != null ? org.getMaxMembers() : plan.getMaxMembers();

        return new EntitlementsResponse(
                plan.getCode(),
                plan.getName(),
                status,
                live.map(Subscription::getTrialEndsAt).orElse(null),
                live.map(Subscription::getCurrentPeriodEnd).orElse(null),
                maxMembers,
                plan.getMaxProjects(),
                plan.getFeatures(),
                organizationMemberRepository.countActiveByOrganizationId(orgId),
                projectRepository.countByOrganizationIdAndStatus(orgId, ProjectStatus.ACTIVE)
        );
    }

    public Optional<Subscription> findLiveSubscription(UUID orgId) {
        return subscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc(orgId, LIVE_STATUSES);
    }

    // ─── Guard metodları — limit aşımında 402'ye dönüşen exception fırlatır ──

    public void assertCanAddMember(UUID orgId) {
        EntitlementsResponse e = getEntitlements(orgId);
        if (e.maxMembers() != null && e.memberCount() >= e.maxMembers()) {
            throw new PlanLimitExceededException("MEMBERS",
                    "Paketinizin üye limitine ulaştınız (" + e.maxMembers() + " üye). " +
                    "Daha fazla üye eklemek için paketinizi yükseltin.");
        }
    }

    public void assertCanCreateProject(UUID orgId) {
        EntitlementsResponse e = getEntitlements(orgId);
        if (e.maxProjects() != null && e.projectCount() >= e.maxProjects()) {
            throw new PlanLimitExceededException("PROJECTS",
                    "Paketinizin proje limitine ulaştınız (" + e.maxProjects() + " proje). " +
                    "Daha fazla proje oluşturmak için paketinizi yükseltin.");
        }
    }

    public void assertFeature(UUID orgId, PlanFeature feature) {
        EntitlementsResponse e = getEntitlements(orgId);
        if (!e.features().contains(feature)) {
            throw new PlanLimitExceededException("FEATURE",
                    "Bu özellik (" + feature + ") mevcut paketinizde bulunmuyor. " +
                    "Kullanmak için paketinizi yükseltin.");
        }
    }

    /** Team/Project üzerinden gelen çağrılar için — org bağlantısı yoksa (legacy veri) gate atlanır. */
    public void assertFeature(Organization org, PlanFeature feature) {
        if (org != null) assertFeature(org.getId(), feature);
    }
}
