package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.enums.MacroTriggerType;
import com.scrumtools.service.collab.macro.CollabMacroService;
import com.scrumtools.service.collab.macro.CollabServerMacroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makro REST yüzeyi (COLLAB_WORKSPACE_PLAN.md §6 — {@code /macros/**}).
 *
 * <p>Burada <b>yürütme yoktur</b>: makro tarayıcıdaki Web Worker'da koşar (K8).
 * Sunucunun payı izin vermek ({@code /run}) ve sonucu denetime yazmak
 * ({@code /runs/{runId}/complete}).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/collab/macros")
@RequiredArgsConstructor
public class CollabMacroController {

    private final CollabMacroService macroService;
    private final CollabServerMacroService serverMacroService;

    @GetMapping
    public ResponseEntity<List<CollabMacroResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID documentId) {
        return ResponseEntity.ok(macroService.list(projectId, documentId));
    }

    @PostMapping
    public ResponseEntity<CollabMacroResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CollabMacroRequest request) {
        return ResponseEntity.ok(macroService.create(projectId, request));
    }

    @PutMapping("/{macroId}")
    public ResponseEntity<CollabMacroResponse> update(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId,
            @Valid @RequestBody CollabMacroRequest request) {
        return ResponseEntity.ok(macroService.update(macroId, request));
    }

    @DeleteMapping("/{macroId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId) {
        macroService.delete(macroId);
        return ResponseEntity.noContent().build();
    }

    /** Onay: kaynağın özetine verilir, kaynak değişince kendiliğinden düşer (§9.2). */
    @PostMapping("/{macroId}/approve")
    public ResponseEntity<CollabMacroResponse> approve(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId) {
        return ResponseEntity.ok(macroService.approve(macroId));
    }

    @PostMapping("/{macroId}/revoke")
    public ResponseEntity<CollabMacroResponse> revoke(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId) {
        return ResponseEntity.ok(macroService.revoke(macroId));
    }

    /**
     * Çalıştırma izni ister. İzin verilirse kaynak ve zaman aşımı döner;
     * verilmezse istek 403 olur ve deneme {@code DENIED} olarak kaydedilir.
     */
    @PostMapping("/{macroId}/run")
    public ResponseEntity<CollabMacroService.RunTicket> run(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId,
            @RequestParam(required = false) MacroTriggerType trigger) {
        return ResponseEntity.ok(macroService.beginRun(macroId, trigger));
    }

    /** İstemci yürütmeyi bitirdiğinde sonucu bildirir. */
    @PostMapping("/runs/{runId}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable UUID projectId,
            @PathVariable UUID runId,
            @Valid @RequestBody CollabMacroRunReport report) {
        macroService.completeRun(runId, report);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{macroId}/runs")
    public ResponseEntity<List<CollabMacroRunResponse>> runs(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId) {
        return ResponseEntity.ok(macroService.runs(macroId));
    }

    /**
     * Webhook sırrını yeniden üretir ve <b>bir kereye mahsus</b> döndürür (Faz 5).
     *
     * <p>Ayrı bir "sırrı göster" ucu bilerek yok: sır yalnızca üretildiği anda
     * görülebilirse, ekran görüntüsü ve destek talebi yoluyla dolaşma ihtimali
     * belirgin şekilde düşer. Kaybeden kullanıcı yenisini üretir — eski adres
     * çalışmayı bırakır, ki zaten istenen budur.
     */
    @PostMapping("/{macroId}/webhook-secret")
    public ResponseEntity<Map<String, String>> rotateWebhookSecret(
            @PathVariable UUID projectId,
            @PathVariable UUID macroId) {
        String secret = serverMacroService.rotateWebhookSecret(macroId);
        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "url", "/api/webhooks/collab-macros/" + macroId,
                "signatureHeader", "X-ScrumTools-Signature",
                "algorithm", "HmacSHA256(hex, gövdenin tamamı)"));
    }
}
