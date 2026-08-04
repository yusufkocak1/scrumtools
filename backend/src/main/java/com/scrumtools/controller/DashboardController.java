package com.scrumtools.controller;

import com.scrumtools.dto.DashboardRequest;
import com.scrumtools.dto.DashboardResponse;
import com.scrumtools.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pano CRUD — takım altında çoklu pano.
 * Base: /api/dashboards
 *
 * Önceki sürüm kullanıcı başına tek düzen tutuyordu ve uçlar da öyleydi
 * ({@code GET /api/dashboards} düz bir widget dizisi dönerdi). Artık liste
 * panoları döner; düzen panonun içindedir.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    /** Takımda görülebilen panolar — kendininkiler + takıma açılanlar. */
    @GetMapping
    public ResponseEntity<List<DashboardResponse>> list(@RequestParam UUID teamId) {
        return ResponseEntity.ok(dashboardService.list(teamId));
    }

    @GetMapping("/{dashboardId}")
    public ResponseEntity<DashboardResponse> get(@PathVariable UUID dashboardId) {
        return ResponseEntity.ok(dashboardService.get(dashboardId));
    }

    @PostMapping
    public ResponseEntity<DashboardResponse> create(@RequestParam UUID teamId,
                                                    @RequestBody DashboardRequest request) {
        return ResponseEntity.ok(dashboardService.create(teamId, request));
    }

    /** Ad / görünürlük / sıra ve — gönderilmişse — düzen. */
    @PutMapping("/{dashboardId}")
    public ResponseEntity<DashboardResponse> update(@PathVariable UUID dashboardId,
                                                    @RequestBody DashboardRequest request) {
        return ResponseEntity.ok(dashboardService.update(dashboardId, request));
    }

    /** Yalnız düzen — sürükle/boyutlandır sonrası çağrılan hafif yol. */
    @PutMapping("/{dashboardId}/layout")
    public ResponseEntity<DashboardResponse> saveLayout(@PathVariable UUID dashboardId,
                                                        @RequestBody List<Map<String, Object>> layout) {
        return ResponseEntity.ok(dashboardService.saveLayout(dashboardId, layout));
    }

    /** Panoyu kendi adına kopyalar; kopya PRIVATE başlar. */
    @PostMapping("/{dashboardId}/duplicate")
    public ResponseEntity<DashboardResponse> duplicate(@PathVariable UUID dashboardId,
                                                       @RequestBody(required = false) DashboardRequest request) {
        return ResponseEntity.ok(
                dashboardService.duplicate(dashboardId, request == null ? null : request.getName()));
    }

    @DeleteMapping("/{dashboardId}")
    public ResponseEntity<Void> delete(@PathVariable UUID dashboardId) {
        dashboardService.delete(dashboardId);
        return ResponseEntity.noContent().build();
    }
}
