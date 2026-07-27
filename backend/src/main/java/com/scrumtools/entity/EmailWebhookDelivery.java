package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * İşlenmiş webhook olaylarının kimlikleri (X-PostForge-Delivery).
 * <p>
 * Aynı olay yeniden denenebildiği için tekrarları yutmakta kullanılır. Idempotency
 * unique kısıtla garanti edilir: ikinci kayıt denemesi çakışır ve olay atlanır.
 * Sayaçlar (openCount/clickCount) bu olmadan her retry'da şişerdi.
 */
@Entity
@Table(name = "email_webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailWebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 120)
    private String deliveryId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime receivedAt;
}
