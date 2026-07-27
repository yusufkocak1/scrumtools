package com.scrumtools.controller;

import com.scrumtools.dto.TaskQueryRequest;
import com.scrumtools.query.QuerySuggestionService;
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
     * Sorguyu yalnızca doğrular. Hata varsa 200 ile birlikte
     * {@code {valid:false, error:{message, position, length}}} döner —
     * editör yazarken çağırdığı için hata bir HTTP hatası değil, veri.
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
