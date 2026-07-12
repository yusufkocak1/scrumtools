package com.scrumtools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Hata raporu yanıtı — rate limit aşımında trackingCode null döner.
 */
@Data
@AllArgsConstructor
public class ErrorReportResponse {
    private String trackingCode;
}
