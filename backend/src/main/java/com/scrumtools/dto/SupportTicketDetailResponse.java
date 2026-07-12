package com.scrumtools.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SupportTicketDetailResponse {

    private SupportTicketResponse ticket;
    private String description;
    private List<SupportMessageResponse> messages;
    private List<SupportAttachmentResponse> attachments;

    /** errorTrackingCode üzerinden çözülen hata grubu (sadece admin detayında dolu) */
    private ErrorGroupResponse linkedErrorGroup;
}
