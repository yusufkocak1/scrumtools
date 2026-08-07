package com.scrumtools.dto;

import com.scrumtools.entity.TaskAttachment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttachmentResponse {

    private String id;
    private String taskId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String uploadedBy;

    /** Geçici (60 dk) MinIO presigned URL — liste/indirme bağlantıları için. */
    private String downloadUrl;

    /** Kalıcı imzalı URL — açıklama/yorum içeriğine gömülecek görseller bunu kullanır. */
    private String mediaUrl;

    private LocalDateTime createdAt;

    public static AttachmentResponse from(TaskAttachment a, String downloadUrl, String mediaUrl) {
        return AttachmentResponse.builder()
                .id(a.getId().toString())
                .taskId(a.getTask().getId().toString())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize())
                .mimeType(a.getMimeType())
                .uploadedBy(a.getUploadedBy())
                .downloadUrl(downloadUrl)
                .mediaUrl(mediaUrl)
                .createdAt(a.getCreatedAt())
                .build();
    }
}

