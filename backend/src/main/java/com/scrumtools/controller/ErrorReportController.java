package com.scrumtools.controller;

import com.scrumtools.dto.ErrorReportRequest;
import com.scrumtools.dto.ErrorReportResponse;
import com.scrumtools.service.ErrorTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@Slf4j
public class ErrorReportController {

    private final ErrorTrackingService errorTrackingService;

    /**
     * POST /api/errors/report
     * Frontend'de yakalanmayan hataları kaydeder ve takip numarası döndürür.
     * Rate limit aşımında trackingCode null döner.
     */
    @PostMapping("/report")
    public ResponseEntity<ErrorReportResponse> report(@Valid @RequestBody ErrorReportRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String trackingCode;
        try {
            trackingCode = errorTrackingService.recordFrontendError(request, userEmail);
        } catch (Exception e) {
            // Hata raporlama asla kullanıcıya hata döndürmemeli
            log.error("[ErrorTracking] Frontend hata raporu kaydedilemedi: {}", e.getMessage(), e);
            trackingCode = null;
        }
        return ResponseEntity.ok(new ErrorReportResponse(trackingCode));
    }
}
