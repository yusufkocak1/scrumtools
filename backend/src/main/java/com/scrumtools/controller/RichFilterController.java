package com.scrumtools.controller;

import com.scrumtools.dto.RichFilterElementRequest;
import com.scrumtools.dto.RichFilterElementResponse;
import com.scrumtools.dto.RichFilterRequest;
import com.scrumtools.dto.RichFilterResponse;
import com.scrumtools.dto.RichFilterRuntimeRequest;
import com.scrumtools.service.RichFilterAlertService;
import com.scrumtools.service.RichFilterAuditService;
import com.scrumtools.service.RichFilterRuntimeService;
import com.scrumtools.service.RichFilterSeriesService;
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
    private final RichFilterRuntimeService runtimeService;
    private final RichFilterSeriesService seriesService;
    private final RichFilterAuditService auditService;
    private final RichFilterAlertService alertService;

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

    // ─── Çalıştırma ───────────────────────────────────────────────────────────
    // Hepsi aynı seçim nesnesini alır; istemci ham sorgu göndermez (K2).
    // Paket denetimi yoktur: paylaşılan bir filtreyi görüntülemek her pakette serbest (K16).

    /** Sayfalı görev listesi + görev başına akıllı filtre etiketi (`smartTags`). */
    @PostMapping("/{richFilterId}/search")
    public ResponseEntity<Map<String, Object>> search(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(runtimeService.search(richFilterId, orEmpty(request)));
    }

    /** Eşleşen kayıt sayısı — sayaç widget'ı. */
    @PostMapping("/{richFilterId}/count")
    public ResponseEntity<Map<String, Object>> count(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(Map.of("count", runtimeService.count(richFilterId, orEmpty(request))));
    }

    /** Gruplama; `groupBy` verilmezse zengin filtrenin kendi akıllı filtreleri eksen olur. */
    @PostMapping("/{richFilterId}/aggregate")
    public ResponseEntity<List<Map<String, Object>>> aggregate(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(runtimeService.aggregate(richFilterId, orEmpty(request)));
    }

    /**
     * Dinamik filtrelerin güncel seçenekleri ve sayıları — kontrol çubuğu için.
     * Her kontrolün seçenekleri kendi seçimi dışlanarak hesaplanır.
     */
    @PostMapping("/{richFilterId}/options")
    public ResponseEntity<List<Map<String, Object>>> options(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(runtimeService.options(richFilterId, orEmpty(request)));
    }

    /** Seçimlerin STQL karşılığı + sayısı — görev listesine geçiş linki için. */
    @PostMapping("/{richFilterId}/resolve")
    public ResponseEntity<Map<String, Object>> resolve(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(runtimeService.resolve(richFilterId, orEmpty(request)));
    }

    /**
     * İki eksenli gruplama — ısı haritası. {@code splitBy} zorunlu, {@code groupBy}
     * verilmezse zengin filtrenin kendi akıllı filtreleri satır ekseni olur.
     */
    @PostMapping("/{richFilterId}/matrix")
    public ResponseEntity<Map<String, Object>> matrix(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        return ResponseEntity.ok(runtimeService.matrix(richFilterId, orEmpty(request)));
    }

    /**
     * Verilen görevlerin akıllı filtre etiketleri — board/liste renklendirmesi.
     * Filtrelemez, yalnız sınıflandırır: görevleri çağıran taraf kendi getirir.
     */
    @PostMapping("/{richFilterId}/classify")
    public ResponseEntity<Map<String, Object>> classify(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestBody(required = false) RichFilterRuntimeRequest request
    ) {
        RichFilterRuntimeRequest body = orEmpty(request);
        return ResponseEntity.ok(
                runtimeService.classifyTasks(richFilterId, body.getTaskIds(), body.getProjectId()));
    }

    /**
     * Sınıflandırma denetimi: hangi kural neyi sahipleniyor, kim kimin görevini
     * alıyor, ne sınıflandırılmadan kalıyor (bkz. RICH_FILTER_PLAN.md — Ö5).
     */
    @GetMapping("/{richFilterId}/audit")
    public ResponseEntity<Map<String, Object>> audit(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestParam(required = false) UUID projectId
    ) {
        return ResponseEntity.ok(auditService.audit(richFilterId, projectId));
    }

    /**
     * Uyarı kuralını şimdi hesaplar — bildirim göndermez, durumu değiştirmez.
     * Kuralı kurarken sayıyı görmek için bildirim tetiklemek doğru olmazdı.
     */
    @PostMapping("/{richFilterId}/alerts/{elementId}/preview")
    public ResponseEntity<Map<String, Object>> previewAlert(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @PathVariable UUID elementId
    ) {
        return ResponseEntity.ok(alertService.preview(richFilterId, elementId));
    }

    /** Gövdesiz istek "seçim yok" demektir — her uç boş seçimle de çalışır. */
    private static RichFilterRuntimeRequest orEmpty(RichFilterRuntimeRequest request) {
        return request != null ? request : new RichFilterRuntimeRequest();
    }

    // ─── Zaman serileri ───────────────────────────────────────────────────────

    /**
     * Zengin filtrenin bütün zaman serileri, tek istekte.
     *
     * Seçim nesnesi almaz: noktalar öğe başına önceden ölçülmüştür, geçmiş
     * çapraz filtrelemeyle yeniden hesaplanamaz (bkz. RICH_FILTER_PLAN.md — K23).
     */
    @GetMapping("/{richFilterId}/series")
    public ResponseEntity<List<Map<String, Object>>> series(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false, defaultValue = "day") String interval
    ) {
        return ResponseEntity.ok(seriesService.series(richFilterId, days, interval));
    }

    /**
     * Geçmişi {@code task_history}'den kurgular.
     *
     * Sorgu izlenmeyen bir alana dayanıyorsa {@code status: "unsupported"} döner —
     * hata değil, sonucun kendisi: seri o zaman bugünden itibaren birikir.
     */
    @PostMapping("/{richFilterId}/series/{elementId}/backfill")
    public ResponseEntity<Map<String, Object>> backfillSeries(
            @SuppressWarnings("unused") @PathVariable UUID teamId,
            @PathVariable UUID richFilterId,
            @PathVariable UUID elementId
    ) {
        return ResponseEntity.ok(seriesService.backfill(richFilterId, elementId));
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
