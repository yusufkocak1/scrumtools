package com.scrumtools.config;

import com.scrumtools.entity.Organization;
import com.scrumtools.entity.Plan;
import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.entity.enums.SubscriptionStatus;
import com.scrumtools.repository.OrganizationRepository;
import com.scrumtools.repository.SubscriptionRepository;
import com.scrumtools.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paket/abonelik altyapısı için başlangıç verisi ve tek seferlik legacy migrasyonu.
 *
 * ddl-auto:update kolon silmediği/rename etmediği için legacy veri düzeltmeleri
 * burada yapılır. Migrasyon, subscriptions tablosu boşken (ilk boot) bir kez çalışır;
 * sonraki boot'larda sadece plan seed'i (idempotent) tekrarlanır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingDataMigrationRunner implements ApplicationRunner {

    private final PlanService planService;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Varsayılan planlar kontrol ediliyor...");
        planService.seedDefaultPlans();

        // Legacy migrasyon daha önce çalıştıysa (herhangi bir subscription varsa) atla.
        if (subscriptionRepository.count() > 0) return;

        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) return;

        Plan trialPlan = planService.getTrialPlan();
        LocalDateTime now = LocalDateTime.now();

        for (Organization org : orgs) {
            // Eski maxMembers=10 varsayılanı artık "override yok" anlamına gelen null olmalı
            if (Integer.valueOf(10).equals(org.getMaxMembers())) {
                org.setMaxMembers(null);
            }

            // Mevcut org'lar deploy'da aniden kısıtlanmasın: hepsine üst paket trial'ı tanımla.
            // Bu aynı zamanda eski 'ENTERPRISE' plan cache değerini de geçerli bir koda çevirir.
            subscriptionRepository.save(Subscription.builder()
                    .organization(org)
                    .plan(trialPlan)
                    .status(SubscriptionStatus.TRIAL)
                    .source(SubscriptionSource.TRIAL)
                    .currentPeriodStart(now)
                    .trialEndsAt(now.plusDays(trialPlan.getTrialDays()))
                    .notes("Billing migrasyonu: mevcut organizasyona otomatik trial tanımlandı.")
                    .build());
            org.setPlan(trialPlan.getCode());
        }
        organizationRepository.saveAll(orgs);
        log.info("Billing migrasyonu: {} mevcut organizasyona {} günlük {} trial tanımlandı.",
                orgs.size(), trialPlan.getTrialDays(), trialPlan.getCode());
    }
}
