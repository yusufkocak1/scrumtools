package com.scrumtools.controller;

import com.scrumtools.dto.report.*;
import com.scrumtools.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Faz 6 — Rapor endpoint'leri
 * Base: /api/teams/{teamId}/reports
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/reports")
public class ReportController {

    private final ReportService reportService;

    /** Takım özet metrikleri (toplam task, open/in-progress/done, overdue, sprint bilgisi, dağılımlar) */
    @GetMapping("/summary")
    public ResponseEntity<TeamSummaryDto> getSummary(@PathVariable UUID teamId) {
        return ResponseEntity.ok(reportService.getTeamSummary(teamId));
    }

    /** Sprint burndown grafiği */
    @GetMapping("/burndown/{sprintId}")
    public ResponseEntity<BurndownReportDto> getBurndown(
            @PathVariable UUID teamId,
            @PathVariable UUID sprintId
    ) {
        return ResponseEntity.ok(reportService.getBurndown(teamId, sprintId));
    }

    /** Tüm sprint'lerin velocity'si */
    @GetMapping("/velocity")
    public ResponseEntity<List<SprintVelocityDto>> getVelocity(@PathVariable UUID teamId) {
        return ResponseEntity.ok(reportService.getVelocity(teamId));
    }

    /** Üye iş yükü (open task count + story points) */
    @GetMapping("/workload")
    public ResponseEntity<List<WorkloadDto>> getWorkload(@PathVariable UUID teamId) {
        return ResponseEntity.ok(reportService.getWorkload(teamId));
    }

    /** Son N gündeki oluşturulan vs çözülen task grafiği. Varsayılan: 30 gün */
    @GetMapping("/created-vs-resolved")
    public ResponseEntity<List<CreatedVsResolvedDto>> getCreatedVsResolved(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(reportService.getCreatedVsResolved(teamId, days));
    }

    /** Vadesi geçmiş görevler listesi */
    @GetMapping("/overdue")
    public ResponseEntity<List<Map<String, Object>>> getOverdue(@PathVariable UUID teamId) {
        return ResponseEntity.ok(reportService.getOverdueTasks(teamId));
    }
}

