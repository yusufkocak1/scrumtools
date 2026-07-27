package com.scrumtools.repository;

import com.scrumtools.entity.EmailWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailWebhookDeliveryRepository extends JpaRepository<EmailWebhookDelivery, UUID> {

    boolean existsByDeliveryId(String deliveryId);
}
