package com.scrumtools.repository;

import com.scrumtools.entity.PaymentTransaction;
import com.scrumtools.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<PaymentTransaction> findByIyzilinkToken(String token);

    Optional<PaymentTransaction> findByConversationId(String conversationId);

    /**
     * Webhook + reconciliation poller aynı anda işleyebilir — satır kilidiyle
     * çifte aktivasyonu önle.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") UUID id);

    List<PaymentTransaction> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime moment);

    Optional<PaymentTransaction> findFirstByOrganizationIdAndStatusOrderByPaidAtDesc(
            UUID organizationId, PaymentStatus status);
}
