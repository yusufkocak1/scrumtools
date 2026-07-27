package com.scrumtools.controller;

import com.scrumtools.service.mail.PostForgeWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PostForge e-posta bildirimleri (SecurityConfig'te /api/webhooks/** zaten permitAll).
 * <p>
 * Gövde HAM string alınır — imza ham byte'lar üzerinden doğrulanır.
 * <p>
 * SCM webhook'larından farklı olarak imza hatasında 401 dönülür: burada durumu telafi
 * edecek bir poller yok, sessiz 200 yanlış yapılandırmayı görünmez kılardı. PostForge
 * 2xx dışını tekrar dener, bu da geçici sır uyumsuzluğunda olayı kurtarır.
 */
@RestController
@RequestMapping("/api/webhooks/postforge")
@RequiredArgsConstructor
public class PostForgeWebhookController {

    private final PostForgeWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-PostForge-Signature", required = false) String signature,
            @RequestHeader(value = "X-PostForge-Delivery", required = false) String deliveryId
    ) {
        boolean accepted = webhookService.handle(rawBody, signature, deliveryId);
        return accepted ? ResponseEntity.ok().build() : ResponseEntity.status(401).build();
    }
}
