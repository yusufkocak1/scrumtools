package com.scrumtools.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumtools.entity.PaymentTransaction;
import com.scrumtools.entity.enums.PaymentStatus;
import com.scrumtools.repository.PaymentTransactionRepository;
import com.scrumtools.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * iyzico ödeme bildirimi webhook'u (SecurityConfig'te /api/webhooks/** permitAll).
 *
 * Güvenlik modeli: payload'a ASLA tek başına güvenilmez — işlem her durumda
 * BillingService.verifyAndActivate içinde iyzico'ya server-to-server sorularak
 * teyit edilir. Bu nedenle sahte webhook en fazla gereksiz bir doğrulama
 * sorgusu tetikler, ödeme aktive edemez. (Kaçan webhook'ları da
 * SubscriptionScheduler'daki reconciliation poller yakalar.)
 *
 * Her zaman hızlı 200 dönülür; aksi halde iyzico bildirimi tekrar tekrar gönderir.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BillingService billingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/iyzico")
    public ResponseEntity<Void> iyzicoWebhook(@RequestBody String rawBody) {
        try {
            Map<?, ?> payload = objectMapper.readValue(rawBody, Map.class);
            log.info("iyzico webhook alındı: {}", payload.get("iyziEventType"));

            findTransaction(payload).ifPresentOrElse(tx -> {
                if (tx.getStatus() == PaymentStatus.PAID) return; // idempotent
                boolean activated = billingService.verifyAndActivate(tx.getId(), rawBody);
                if (!activated) {
                    log.warn("Webhook geldi ama iyzico teyidi 'ödendi' demiyor: tx={}", tx.getId());
                }
            }, () -> log.warn("Webhook eşleşen ödeme kaydı bulunamadı: {}", rawBody));
        } catch (Exception e) {
            // 200 dışı dönmek iyzico'nun tekrar denemesine yol açar; hatayı logla, poller telafi eder
            log.error("iyzico webhook işlenemedi: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /** Payload'daki conversationId veya token ile işlemi bul (alan adları sürüme göre değişebiliyor). */
    private Optional<PaymentTransaction> findTransaction(Map<?, ?> payload) {
        for (String key : new String[]{"paymentConversationId", "conversationId"}) {
            Object v = payload.get(key);
            if (v != null) {
                Optional<PaymentTransaction> tx = paymentTransactionRepository.findByConversationId(String.valueOf(v));
                if (tx.isPresent()) return tx;
            }
        }
        Object token = payload.get("token");
        if (token != null) {
            return paymentTransactionRepository.findByIyzilinkToken(String.valueOf(token));
        }
        return Optional.empty();
    }
}
