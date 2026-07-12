package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.SupportTicketAttachment;
import com.scrumtools.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Kullanıcıya açık destek talebi endpoint'leri — herkes sadece kendi taleplerini görür.
 */
@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody SupportTicketCreateRequest request) {
        return ResponseEntity.ok(supportTicketService.createTicket(currentUserEmail(), request));
    }

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> myTickets() {
        return ResponseEntity.ok(supportTicketService.getMyTickets(currentUserEmail()));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<SupportTicketDetailResponse> detail(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(supportTicketService.getMyTicketDetail(currentUserEmail(), ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<SupportMessageResponse> addMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportTicketService.addUserMessage(currentUserEmail(), ticketId, request));
    }

    @PostMapping(value = "/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportAttachmentResponse> uploadAttachment(
            @PathVariable UUID ticketId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(supportTicketService.uploadAttachment(currentUserEmail(), ticketId, file));
    }

    /** Dosyayı backend üzerinden indir (proxy) — talep sahibi veya SUPER_ADMIN. */
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        SupportTicketAttachment attachment =
                supportTicketService.getAttachmentForDownload(currentUserEmail(), ticketId, attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        attachment.getMimeType() != null ? attachment.getMimeType() : "application/octet-stream"))
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(supportTicketService.downloadAttachment(attachment)));
    }

    @DeleteMapping("/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        supportTicketService.deleteAttachment(currentUserEmail(), ticketId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
