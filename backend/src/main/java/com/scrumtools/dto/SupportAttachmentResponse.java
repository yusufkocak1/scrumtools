package com.scrumtools.dto;

import com.scrumtools.entity.SupportTicketAttachment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportAttachmentResponse {

    private String id;
    private String ticketId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String uploadedBy;
    private String downloadUrl;
    private LocalDateTime createdAt;

    public static SupportAttachmentResponse from(SupportTicketAttachment a, String downloadUrl) {
        return SupportAttachmentResponse.builder()
                .id(a.getId().toString())
                .ticketId(a.getTicket().getId().toString())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize())
                .mimeType(a.getMimeType())
                .uploadedBy(a.getUploadedBy())
                .downloadUrl(downloadUrl)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
