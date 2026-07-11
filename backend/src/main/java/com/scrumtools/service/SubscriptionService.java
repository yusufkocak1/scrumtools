package com.scrumtools.service;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.OrganizationMember;
import com.scrumtools.entity.Plan;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.BillingCycle;
import com.scrumtools.entity.enums.OrgRole;
import com.scrumtools.entity.enums.ProjectStatus;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.entity.enums.SubscriptionStatus;
import com.scrumtools.repository.OrganizationMemberRepository;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.SubscriptionRepository;
import com.scrumtools.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abonelik yaşam döngüsünün tek merkezi.
 *
 * activate(): webhook, reconciliation poller ve superadmin manuel aktivasyonun
 * ORTAK kod yolu — üçü de buradan geçer. Aktivasyon; dönemi uzatır/başlatır,
 * Organization.plan cache'ini günceller, downgrade ile pasifleştirilenleri geri açar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final PlanService planService;
    private final MailService mailService;

    /**
     * Aboneliği aktive eder / yeniler / plan değiştirir.
     *
     * @param overridePeriodEnd null değilse dönem sonu bu tarihe ayarlanır (superadmin özel uzatma)
     */
    @Transactional
    public Subscription activate(UUID orgId, String planCode, BillingCycle cycle,
                                 SubscriptionSource source, User actor, LocalDateTime overridePeriodEnd) {
        Organization org = getOrg(orgId);
        Plan plan = planService.getByCode(planCode);
        LocalDateTime now = LocalDateTime.now();

        Subscription sub = findLive(orgId).orElse(null);
        if (sub != null) {
            boolean samePlanRenewal = sub.getStatus() != SubscriptionStatus.TRIAL
                    && sub.getPlan().getId().equals(plan.getId());
            if (samePlanRenewal) {
                // Aynı plan yenileme: kalan süre kaybolmaz, dönem sonundan devam eder
                LocalDateTime base = sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().isAfter(now)
                        ? sub.getCurrentPeriodEnd() : now;
                sub.setCurrentPeriodEnd(overridePeriodEnd != null ? overridePeriodEnd : addCycle(base, cycle));
            } else {
                // Trial→paid dönüşümü veya plan değişimi: yeni dönem şimdi başlar
                sub.setPlan(plan);
                sub.setCurrentPeriodStart(now);
                sub.setCurrentPeriodEnd(overridePeriodEnd != null ? overridePeriodEnd : addCycle(now, cycle));
            }
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setBillingCycle(cycle);
            sub.setTrialEndsAt(null);
        } else {
            sub = Subscription.builder()
                    .organization(org)
                    .plan(plan)
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(cycle)
                    .currentPeriodStart(now)
                    .currentPeriodEnd(overridePeriodEnd != null ? overridePeriodEnd : addCycle(now, cycle))
                    .build();
        }
        sub.setSource(source);
        sub.setActivatedBy(actor);
        sub.setRemindersSent(0);
        sub = subscriptionRepository.save(sub);

        org.setPlan(plan.getCode());
        organizationRepository.save(org);

        restoreDowngraded(org, plan);

        mailService.sendPaymentReceived(org, plan.getName(), sub.getCurrentPeriodEnd());
        log.info("Abonelik aktive edildi: org='{}' plan={} cycle={} source={} dönem sonu={}",
                org.getSlug(), plan.getCode(), cycle, source, sub.getCurrentPeriodEnd());
        return sub;
    }

    /** Superadmin: trial'ı uzat (canlı trial yoksa yeni trial başlatır). */
    @Transactional
    public Subscription extendTrial(UUID orgId, int days, User actor) {
        Organization org = getOrg(orgId);
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = findLive(orgId).orElse(null);

        if (sub != null && sub.getStatus() == SubscriptionStatus.TRIAL) {
            LocalDateTime base = sub.getTrialEndsAt() != null && sub.getTrialEndsAt().isAfter(now)
                    ? sub.getTrialEndsAt() : now;
            sub.setTrialEndsAt(base.plusDays(days));
        } else if (sub == null) {
            Plan trialPlan = planService.getTrialPlan();
            sub = Subscription.builder()
                    .organization(org)
                    .plan(trialPlan)
                    .status(SubscriptionStatus.TRIAL)
                    .source(SubscriptionSource.MANUAL)
                    .currentPeriodStart(now)
                    .trialEndsAt(now.plusDays(days))
                    .build();
            org.setPlan(trialPlan.getCode());
            organizationRepository.save(org);
            restoreDowngraded(org, trialPlan);
        } else {
            throw new IllegalStateException("Bu organizasyonun aktif (ücretli) aboneliği var; trial uzatılamaz.");
        }
        sub.setActivatedBy(actor);
        return subscriptionRepository.save(sub);
    }

    /** Superadmin: aboneliği iptal et — org anında FREE'ye düşer. */
    @Transactional
    public void cancel(UUID orgId, User actor) {
        Subscription sub = findLive(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Bu organizasyonun canlı aboneliği yok."));
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(LocalDateTime.now());
        sub.setActivatedBy(actor);
        subscriptionRepository.save(sub);
        downgradeToFree(sub.getOrganization());
        log.info("Abonelik iptal edildi: org='{}'", sub.getOrganization().getSlug());
    }

    /**
     * Org'u FREE limitlerine düşürür: veri silinmez; limit üstü üyeler pasifleşir,
     * limit üstü (en yeni) projeler arşivlenir. Yeniden aktivasyonda geri açılır.
     */
    @Transactional
    public void downgradeToFree(Organization org) {
        Plan free = planService.getDefaultPlan();
        org.setPlan(free.getCode());
        organizationRepository.save(org);

        Integer maxMembers = org.getMaxMembers() != null ? org.getMaxMembers() : free.getMaxMembers();
        if (maxMembers != null) {
            List<OrganizationMember> members =
                    organizationMemberRepository.findByOrganizationIdOrderByJoinedAtAsc(org.getId());
            // Sahip her zaman kalır; sonra en eski üyeler limit dolana kadar
            int kept = 0;
            for (OrganizationMember m : members) {
                boolean isOwner = m.getOrgRole() == OrgRole.ORG_OWNER;
                if (!m.isActiveMember()) continue; // zaten pasif
                if (isOwner || kept < maxMembers) {
                    kept++;
                } else {
                    m.setActive(false);
                    m.setDeactivatedByDowngrade(true);
                }
            }
            organizationMemberRepository.saveAll(members);
        }

        Integer maxProjects = free.getMaxProjects();
        if (maxProjects != null) {
            List<Project> activeProjects = projectRepository
                    .findByOrganizationIdAndStatus(org.getId(), ProjectStatus.ACTIVE).stream()
                    .sorted(Comparator.comparing(Project::getCreatedAt))
                    .toList();
            for (int i = maxProjects; i < activeProjects.size(); i++) {
                Project p = activeProjects.get(i);
                p.setStatus(ProjectStatus.ARCHIVED);
                p.setArchivedByDowngrade(true);
            }
            projectRepository.saveAll(activeProjects);
        }
        log.info("Org FREE'ye düşürüldü: '{}'", org.getSlug());
    }

    /** Downgrade nedeniyle pasifleştirilenleri yeni plan limitlerine kadar geri açar. */
    private void restoreDowngraded(Organization org, Plan plan) {
        Integer maxMembers = org.getMaxMembers() != null ? org.getMaxMembers() : plan.getMaxMembers();
        List<OrganizationMember> members =
                organizationMemberRepository.findByOrganizationIdOrderByJoinedAtAsc(org.getId());
        long activeCount = members.stream().filter(OrganizationMember::isActiveMember).count();
        for (OrganizationMember m : members) {
            if (Boolean.TRUE.equals(m.getDeactivatedByDowngrade())
                    && (maxMembers == null || activeCount < maxMembers)) {
                m.setActive(true);
                m.setDeactivatedByDowngrade(false);
                activeCount++;
            }
        }
        organizationMemberRepository.saveAll(members);

        Integer maxProjects = plan.getMaxProjects();
        List<Project> archived = projectRepository
                .findByOrganizationIdAndStatus(org.getId(), ProjectStatus.ARCHIVED).stream()
                .filter(p -> Boolean.TRUE.equals(p.getArchivedByDowngrade()))
                .sorted(Comparator.comparing(Project::getCreatedAt))
                .toList();
        long activeProjects = projectRepository.countByOrganizationIdAndStatus(org.getId(), ProjectStatus.ACTIVE);
        for (Project p : archived) {
            if (maxProjects == null || activeProjects < maxProjects) {
                p.setStatus(ProjectStatus.ACTIVE);
                p.setArchivedByDowngrade(false);
                activeProjects++;
            }
        }
        projectRepository.saveAll(archived);
    }

    public Optional<Subscription> findLive(UUID orgId) {
        return subscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc(
                orgId, EntitlementService.LIVE_STATUSES);
    }

    private Organization getOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organizasyon bulunamadı: " + orgId));
    }

    private LocalDateTime addCycle(LocalDateTime from, BillingCycle cycle) {
        if (cycle == null) cycle = BillingCycle.MONTHLY;
        return cycle == BillingCycle.YEARLY ? from.plusYears(1) : from.plusMonths(1);
    }
}
