package com.scrumtools.dto;

import com.scrumtools.entity.ErrorEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorEventResponse {

    private String id;
    private String trackingCode;
    private String groupId;
    private String userEmail;
    private String source;
    private String message;
    private String stackTrace;
    private String endpoint;
    private String httpMethod;
    private String userAgent;
    private LocalDateTime occurredAt;

    public static ErrorEventResponse from(ErrorEvent e) {
        return ErrorEventResponse.builder()
                .id(e.getId().toString())
                .trackingCode(e.getTrackingCode())
                .groupId(e.getGroup().getId().toString())
                .userEmail(e.getUserEmail())
                .source(e.getSource().name())
                .message(e.getMessage())
                .stackTrace(e.getStackTrace())
                .endpoint(e.getEndpoint())
                .httpMethod(e.getHttpMethod())
                .userAgent(e.getUserAgent())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
