package com.scrumtools.controller;

import com.scrumtools.dto.SavedFilterRequest;
import com.scrumtools.dto.SavedFilterResponse;
import com.scrumtools.service.SavedFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kayıtlı filtre uçları.
 * Base: /api/teams/{teamId}/filters
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/filters")
public class SavedFilterController {

    private final SavedFilterService savedFilterService;

    /** Kullanıcının görebildiği filtreler — favoriler üstte. */
    @GetMapping
    public ResponseEntity<List<SavedFilterResponse>> list(
            @PathVariable UUID teamId,
            @RequestParam(required = false) UUID projectId
    ) {
        return ResponseEntity.ok(savedFilterService.list(teamId, projectId));
    }

    @GetMapping("/{filterId}")
    public ResponseEntity<SavedFilterResponse> get(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId
    ) {
        return ResponseEntity.ok(savedFilterService.get(filterId));
    }

    @PostMapping
    public ResponseEntity<SavedFilterResponse> create(
            @PathVariable UUID teamId,
            @Valid @RequestBody SavedFilterRequest request
    ) {
        return ResponseEntity.ok(savedFilterService.create(teamId, request));
    }

    @PutMapping("/{filterId}")
    public ResponseEntity<SavedFilterResponse> update(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId,
            @Valid @RequestBody SavedFilterRequest request
    ) {
        return ResponseEntity.ok(savedFilterService.update(filterId, request));
    }

    @DeleteMapping("/{filterId}")
    public ResponseEntity<Void> delete(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId
    ) {
        savedFilterService.delete(filterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{filterId}/favorite")
    public ResponseEntity<SavedFilterResponse> addFavorite(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId
    ) {
        return ResponseEntity.ok(savedFilterService.toggleFavorite(filterId, true));
    }

    @DeleteMapping("/{filterId}/favorite")
    public ResponseEntity<SavedFilterResponse> removeFavorite(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId
    ) {
        return ResponseEntity.ok(savedFilterService.toggleFavorite(filterId, false));
    }

    /** Kayıtlı filtreyi çalıştırır ve sayfalı görev listesi döner. */
    @PostMapping("/{filterId}/run")
    public ResponseEntity<Map<String, Object>> run(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Map<String, Object> params = body != null ? body : Map.of();
        UUID projectId = params.get("projectId") != null
                ? UUID.fromString(params.get("projectId").toString()) : null;
        Integer page = params.get("page") != null
                ? Integer.valueOf(params.get("page").toString()) : null;
        Integer size = params.get("size") != null
                ? Integer.valueOf(params.get("size").toString()) : null;

        return ResponseEntity.ok(savedFilterService.run(filterId, projectId, page, size));
    }

    /** Kayıtlı filtrenin eşleşen kayıt sayısı — sayaç widget'ları için. */
    @PostMapping("/{filterId}/count")
    public ResponseEntity<Map<String, Object>> count(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Map<String, Object> params = body != null ? body : Map.of();
        long total = savedFilterService.count(filterId, uuidOrNull(params.get("projectId")));
        return ResponseEntity.ok(Map.of("count", total));
    }

    /** Kayıtlı filtre sonucunu bir alana göre gruplar — grafik widget'ları için. */
    @PostMapping("/{filterId}/aggregate")
    public ResponseEntity<List<Map<String, Object>>> aggregate(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID filterId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(savedFilterService.aggregate(
                filterId,
                uuidOrNull(body.get("projectId")),
                str(body.get("groupBy")),
                str(body.get("metric")),
                body.get("limit") != null ? Integer.valueOf(body.get("limit").toString()) : null));
    }

    private static UUID uuidOrNull(Object value) {
        return value != null ? UUID.fromString(value.toString()) : null;
    }

    private static String str(Object value) {
        return value != null ? value.toString() : null;
    }
}
