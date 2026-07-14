package com.scrumtools.controller;

import com.scrumtools.service.scm.ScmWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * SCM push webhook'ları (SecurityConfig'te /api/webhooks/** zaten permitAll).
 *
 * Gövde HAM string olarak alınır — imza doğrulaması ham byte'lar üzerinden
 * yapılmak zorundadır (parse sonrası yeniden serialize etmek imzayı bozar).
 * Her durumda hızlı 200 dönülür; doğrulanamayan istekler sessizce loglanıp
 * yutulur (iyzico webhook felsefesi: saldırgana bilgi sızdırma, sağlayıcının
 * retry fırtınasını tetikleme).
 */
@RestController
@RequestMapping("/api/webhooks/scm")
@RequiredArgsConstructor
@Slf4j
public class ScmWebhookController {

    private final ScmWebhookService webhookService;

    @PostMapping("/{provider}/{connectionId}")
    public ResponseEntity<Void> handle(
            @PathVariable String provider,
            @PathVariable UUID connectionId,
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers
    ) {
        try {
            webhookService.handle(provider, connectionId, rawBody, headers);
        } catch (Exception e) {
            // 200 dışı dönmek sağlayıcının tekrar denemesine yol açar; logla, poller telafi eder
            log.error("SCM webhook işlenemedi (connection={}): {}", connectionId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
