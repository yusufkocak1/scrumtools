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
        /** Geçici (60 dk) MinIO presigned URL — liste/indirme bağlantıları için. */
        String downloadUrl,
        /** Kalıcı imzalı URL — sayfa içeriğine gömülecek görseller bunu kullanır. */
        String mediaUrl,
        String uploadedByEmail,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public static DocAttachmentResponse from(DocPageAttachment a, String downloadUrl, String mediaUrl) {
        return new DocAttachmentResponse(
                a.getId(),
                a.getPage().getId(),
                a.getFileName(),
                a.getFileSize(),
                a.getMimeType(),
                downloadUrl,
                mediaUrl,
                a.getUploadedBy().getEmail(),
                a.getUploadedBy().getName(),
                a.getCreatedAt()
        );
    }
}

