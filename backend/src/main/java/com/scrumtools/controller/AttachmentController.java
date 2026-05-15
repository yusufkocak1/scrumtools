package com.scrumtools.controller;

import com.scrumtools.dto.AttachmentResponse;
import com.scrumtools.entity.TaskAttachment;
import com.scrumtools.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * POST /api/teams/{teamId}/tasks/{taskId}/attachments
     * Dosya yükle (multipart/form-data)
     */
    @PostMapping(value = "/api/teams/{teamId}/tasks/{taskId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(attachmentService.upload(teamId, taskId, file));
    }

    /**
     * GET /api/teams/{teamId}/tasks/{taskId}/attachments
     * Task'ın tüm dosyalarını listele (pre-signed URL ile)
     */
    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> list(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(attachmentService.getAttachments(teamId, taskId));
    }

    /**
     * GET /api/teams/{teamId}/tasks/{taskId}/attachments/{attachmentId}/download
     * Dosyayı backend üzerinden indir (proxy)
     */
    @GetMapping("/api/teams/{teamId}/tasks/{taskId}/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId
    ) {
        TaskAttachment attachment = attachmentService.getAttachmentEntity(attachmentId);
        InputStream stream = attachmentService.download(attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        attachment.getMimeType() != null ? attachment.getMimeType() : "application/octet-stream"))
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(stream));
    }

    /**
     * DELETE /api/teams/{teamId}/tasks/{taskId}/attachments/{attachmentId}
     * Dosyayı sil (MinIO + DB)
     */
    @DeleteMapping("/api/teams/{teamId}/tasks/{taskId}/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId
    ) {
        attachmentService.delete(teamId, taskId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}

