package com.scrumtools.repository;

import com.scrumtools.entity.Subscription;
import com.scrumtools.entity.enums.SubscriptionSource;
import com.scrumtools.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** Org'un canlı aboneliği (TRIAL/ACTIVE/PAST_DUE) — en fazla bir tane olmalı. */
    Optional<Subscription> findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId, Collection<SubscriptionStatus> statuses);

    List<Subscription> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    boolean existsByOrganizationIdAndStatusIn(UUID organizationId, Collection<SubscriptionStatus> statuses);

    /** Trial suistimali kontrolü: bu kullanıcının sahibi olduğu org'larda daha önce trial başlatılmış mı? */
    boolean existsByOrganizationOwnerIdAndSource(UUID ownerId, SubscriptionSource source);

    // ─── Scheduler sorguları (Faz 4) ─────────────────────────────────────────

    List<Subscription> findByStatusAndTrialEndsAtBefore(SubscriptionStatus status, LocalDateTime moment);

    List<Subscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, LocalDateTime moment);

    List<Subscription> findByStatusIn(Collection<SubscriptionStatus> statuses);
}
