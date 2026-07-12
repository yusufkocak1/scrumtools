package com.scrumtools.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Frontend'den gelen hata raporu (window.onerror / unhandledrejection / Vue errorHandler).
 */
@Data
public class ErrorReportRequest {

    @Size(max = 256)
    private String errorName;

    @Size(max = 4000)
    private String message;

    @Size(max = 20000)
    private String stack;

    @Size(max = 1024)
    private String url;

    @Size(max = 256)
    private String componentName;

    @Size(max = 512)
    private String userAgent;
}
