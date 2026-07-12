package com.scrumtools.dto;

import com.scrumtools.entity.SupportTicket;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketResponse {

    private String id;
    private String subject;
    private String category;
    private String status;
    private String userEmail;
    private String userName;
    private String organizationId;
    private String organizationName;
    private String errorTrackingCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupportTicketResponse from(SupportTicket t) {
        return SupportTicketResponse.builder()
                .id(t.getId().toString())
                .subject(t.getSubject())
                .category(t.getCategory().name())
                .status(t.getStatus().name())
                .userEmail(t.getUser().getEmail())
                .userName(t.getUser().getName())
                .organizationId(t.getOrganizationId() != null ? t.getOrganizationId().toString() : null)
                .organizationName(t.getOrganizationName())
                .errorTrackingCode(t.getErrorTrackingCode())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
