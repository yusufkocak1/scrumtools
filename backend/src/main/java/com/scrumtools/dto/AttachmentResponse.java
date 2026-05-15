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
    private String downloadUrl;
    private LocalDateTime createdAt;

    public static AttachmentResponse from(TaskAttachment a, String downloadUrl) {
        return AttachmentResponse.builder()
                .id(a.getId().toString())
                .taskId(a.getTask().getId().toString())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize())
                .mimeType(a.getMimeType())
                .uploadedBy(a.getUploadedBy())
                .downloadUrl(downloadUrl)
                .createdAt(a.getCreatedAt())
                .build();
    }
}

