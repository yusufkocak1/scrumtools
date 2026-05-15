package com.scrumtools.dto;

import com.scrumtools.entity.DocPageAttachment;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocAttachmentResponse(
        UUID id,
        UUID pageId,
        String fileName,
        long fileSize,
        String mimeType,
        String downloadUrl,
        String uploadedByEmail,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public static DocAttachmentResponse from(DocPageAttachment a, String downloadUrl) {
        return new DocAttachmentResponse(
                a.getId(),
                a.getPage().getId(),
                a.getFileName(),
                a.getFileSize(),
                a.getMimeType(),
                downloadUrl,
                a.getUploadedBy().getEmail(),
                a.getUploadedBy().getName(),
                a.getCreatedAt()
        );
    }
}

