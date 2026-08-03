package com.scrumtools.controller;

import com.scrumtools.dto.RichFilterElementRequest;
import com.scrumtools.dto.RichFilterElementResponse;
import com.scrumtools.dto.RichFilterRequest;
import com.scrumtools.dto.RichFilterResponse;
import com.scrumtools.service.RichFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Zengin filtre tanım uçları.
 * Base: /api/teams/{teamId}/rich-filters
 *
 * Çalıştırma uçları (search/count/aggregate/options) sonraki fazlarda eklenecek;
 * bu fazda grafik verisi mevcut {@code /tasks/query/aggregate} ucundan alınır —
 * {@code groupBy: smart["zengin filtre adı"]} yazımıyla.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/rich-filters")
public class RichFilterController {

    private final RichFilterService richFilterService;

    @GetMapping
    public ResponseEntity<List<RichFilterResponse>> list(
            @PathVariable UUID teamId,
            @RequestParam(required = false) UUID projectId
    ) {
        return ResponseEntity.ok(richFilterService.list(teamId, projectId));
    }

    @GetMapping("/{richFilterId}")
    public ResponseEntity<RichFilterResponse> get(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId
    ) {
        return ResponseEntity.ok(richFilterService.get(richFilterId));
    }

    @PostMapping
    public ResponseEntity<RichFilterResponse> create(
            @PathVariable UUID teamId,
            @Valid @RequestBody RichFilterRequest request
    ) {
        return ResponseEntity.ok(richFilterService.create(teamId, request));
    }

    @PutMapping("/{richFilterId}")
    public ResponseEntity<RichFilterResponse> update(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @Valid @RequestBody RichFilterRequest request
    ) {
        return ResponseEntity.ok(richFilterService.update(richFilterId, request));
    }

    @DeleteMapping("/{richFilterId}")
    public ResponseEntity<Void> delete(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId
    ) {
        richFilterService.delete(richFilterId);
        return ResponseEntity.noContent().build();
    }

    /** Öğeleriyle birlikte kopyalar — kopya her zaman kopyalayanın PRIVATE kaydıdır. */
    @PostMapping("/{richFilterId}/duplicate")
    public ResponseEntity<RichFilterResponse> duplicate(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId
    ) {
        return ResponseEntity.ok(richFilterService.duplicate(richFilterId));
    }

    // ─── Öğeler ───────────────────────────────────────────────────────────────

    @PostMapping("/{richFilterId}/elements")
    public ResponseEntity<RichFilterElementResponse> addElement(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @Valid @RequestBody RichFilterElementRequest request
    ) {
        return ResponseEntity.ok(richFilterService.addElement(richFilterId, request));
    }

    @PutMapping("/{richFilterId}/elements/{elementId}")
    public ResponseEntity<RichFilterElementResponse> updateElement(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @PathVariable UUID elementId,
            @Valid @RequestBody RichFilterElementRequest request
    ) {
        return ResponseEntity.ok(richFilterService.updateElement(richFilterId, elementId, request));
    }

    @DeleteMapping("/{richFilterId}/elements/{elementId}")
    public ResponseEntity<Void> deleteElement(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @PathVariable UUID elementId
    ) {
        richFilterService.deleteElement(richFilterId, elementId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Öğe sırasını değiştirir. Akıllı filtrelerde sıra sonucu belirler:
     * bir görev, kendisine uyan ilk akıllı filtrenin rengini ve etiketini alır.
     */
    @PutMapping("/{richFilterId}/elements/reorder")
    public ResponseEntity<List<RichFilterElementResponse>> reorder(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody Map<String, List<UUID>> body
    ) {
        List<UUID> ids = body.getOrDefault("elementIds", List.of());
        return ResponseEntity.ok(richFilterService.reorderElements(richFilterId, ids));
    }
}
