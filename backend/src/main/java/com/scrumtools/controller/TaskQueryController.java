package com.scrumtools.controller;

import com.scrumtools.dto.TaskAggregateRequest;
import com.scrumtools.dto.TaskQueryRequest;
import com.scrumtools.query.QuerySuggestionService;
import com.scrumtools.query.TaskAggregationService;
import com.scrumtools.query.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * STQL (ScrumTools Query Language) uçları.
 *
 * Eski {@code POST /tasks/filter} ucu geriye dönük uyum için yerinde kalır;
 * o da aynı sorgu motorunu kullanır.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/tasks/query")
public class TaskQueryController {

    private final TaskQueryService taskQueryService;
    private final TaskAggregationService aggregationService;
    private final QuerySuggestionService suggestionService;

    /** Sorguyu çalıştırır ve sayfalı sonuç döner. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> run(
            @PathVariable UUID teamId,
            @RequestBody TaskQueryRequest request
    ) {
        return ResponseEntity.ok(taskQueryService.search(
                teamId, request.getProjectId(), request.getQuery(),
                request.getPage(), request.getSize()));
    }

    /**
     * Sorguyu doğrular ve geçerliyse eşleşen kayıt sayısını da döner:
     * {@code {valid:true, count:24}} veya
     * {@code {valid:false, error:{message, position, length}}}.
     *
     * Her iki durumda da 200 döner — editör yazarken çağırdığı için yarım kalmış
     * bir sorgu HTTP hatası değil, beklenen bir durumdur.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @PathVariable UUID teamId,
            @RequestBody TaskQueryRequest request
    ) {
        return ResponseEntity.ok(
                taskQueryService.validate(teamId, request.getProjectId(), request.getQuery()));
    }

    /** Eşleşen kayıt sayısı — editördeki canlı "N sonuç" göstergesi için. */
    @PostMapping("/count")
    public ResponseEntity<Map<String, Object>> count(
            @PathVariable UUID teamId,
            @RequestBody TaskQueryRequest request
    ) {
        long total = taskQueryService.count(teamId, request.getProjectId(), request.getQuery());
        return ResponseEntity.ok(Map.of("count", total));
    }

    /**
     * Sonucu bir alana göre gruplayıp sayar veya toplar — grafik widget'ları için.
     * Dönen her kova, kendisine daraltan STQL parçasını da taşır ({@code filter});
     * arayüz grafik diliminden görev listesine bu parçayla geçer.
     */
    @PostMapping("/aggregate")
    public ResponseEntity<List<Map<String, Object>>> aggregate(
            @PathVariable UUID teamId,
            @RequestBody TaskAggregateRequest request
    ) {
        return ResponseEntity.ok(aggregationService.aggregate(
                teamId, request.getProjectId(), request.getQuery(),
                request.getGroupBy(), request.getMetric(), request.getLimit()));
    }

    /** Sorgulanabilir alanlar, operatörleri ve fonksiyon kataloğu. */
    @GetMapping("/fields")
    public ResponseEntity<Map<String, Object>> fields(@PathVariable UUID teamId) {
        return ResponseEntity.ok(suggestionService.fieldCatalog());
    }

    /** Bir alan için değer önerileri (otomatik tamamlama). */
    @GetMapping("/suggest")
    public ResponseEntity<List<Map<String, String>>> suggest(
            @PathVariable UUID teamId,
            @RequestParam String field,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false, defaultValue = "") String prefix
    ) {
        return ResponseEntity.ok(suggestionService.suggestValues(teamId, projectId, field, prefix));
    }
}
