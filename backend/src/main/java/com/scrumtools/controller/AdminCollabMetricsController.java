package com.scrumtools.controller;

import com.scrumtools.dto.CollabMetricsResponse;
import com.scrumtools.repository.CollabUpdateRepository;
import com.scrumtools.service.collab.CollabUpdateBatcher;
import com.scrumtools.service.collab.macro.CollabMacroQueue;
import com.scrumtools.websocket.CollabSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ortak çalışma alanı kaynak izleme ekranının verisi (plan §12, Faz 5).
 *
 * <p>Yalnızca SUPER_ADMIN: sayılar tüm kiracıları kapsıyor ve doküman
 * başlıklarını içeriyor.
 *
 * <p>Sorgular <b>istek üzerine</b> çalışır, arka planda toplanmaz. D3'ün dar
 * sunucusunda sürekli metrik toplayan bir zamanlanmış görev, ölçmeye çalıştığı
 * yükün kendisi olurdu.
 */
@RestController
@RequestMapping("/api/admin/collab/metrics")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isSuperAdmin(authentication)")
public class AdminCollabMetricsController {

    private final CollabSessionRegistry sessionRegistry;
    private final CollabUpdateBatcher batcher;
    private final CollabUpdateRepository updateRepository;
    private final CollabMacroQueue macroQueue;

    @GetMapping
    public ResponseEntity<CollabMetricsResponse> metrics() {
        Runtime runtime = Runtime.getRuntime();

        return ResponseEntity.ok(new CollabMetricsResponse(
                sessionRegistry.openConnectionCount(),
                sessionRegistry.openDocumentCount(),
                batcher.acceptedUpdates(),
                batcher.bufferedBytes(),
                updateRepository.count(),
                macroQueue.queueDepth(),
                macroQueue.activeCount(),
                runtime.totalMemory() - runtime.freeMemory(),
                runtime.maxMemory(),
                updateRepository.findTopDocumentsByPayload().stream()
                        .map(row -> new CollabMetricsResponse.TopDocument(
                                row.getDocumentId(), row.getTitle(),
                                row.getUpdateCount(), row.getPayloadBytes()))
                        .toList()));
    }
}
