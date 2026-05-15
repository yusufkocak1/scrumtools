package com.scrumtools.controller;

import com.scrumtools.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Faz 6 — Kişisel dashboard layout CRUD
 * Base: /api/dashboards
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLayout() {
        return ResponseEntity.ok(dashboardService.getLayout());
    }

    @PutMapping
    public ResponseEntity<List<Map<String, Object>>> saveLayout(
            @RequestBody List<Map<String, Object>> layout
    ) {
        return ResponseEntity.ok(dashboardService.saveLayout(layout));
    }

    @PutMapping("/widgets")
    public ResponseEntity<List<Map<String, Object>>> upsertWidget(
            @RequestBody Map<String, Object> widget
    ) {
        return ResponseEntity.ok(dashboardService.upsertWidget(widget));
    }

    @DeleteMapping("/widgets/{widgetId}")
    public ResponseEntity<List<Map<String, Object>>> removeWidget(@PathVariable String widgetId) {
        return ResponseEntity.ok(dashboardService.removeWidget(widgetId));
    }
}

