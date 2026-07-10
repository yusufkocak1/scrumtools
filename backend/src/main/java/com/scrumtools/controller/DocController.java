package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.DocTargetType;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.service.DocAttachmentService;
import com.scrumtools.service.DocPageService;
import com.scrumtools.service.DocPermissionService;
import com.scrumtools.service.DocSpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/docs")
@RequiredArgsConstructor
public class DocController {

    private final DocSpaceService spaceService;
    private final DocPageService pageService;
    private final DocPermissionService permissionService;
    private final DocAttachmentService attachmentService;
    private final UserRepository userRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // SPACES
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/spaces")
    public ResponseEntity<DocSpaceResponse> createSpace(
            @PathVariable UUID projectId,
            @Valid @RequestBody DocSpaceRequest request) {
        return ResponseEntity.ok(spaceService.createSpace(projectId, request));
    }

    @GetMapping("/spaces")
    public ResponseEntity<List<DocSpaceResponse>> getSpaces(@PathVariable UUID projectId) {
        return ResponseEntity.ok(spaceService.getSpaces(projectId));
    }

    @GetMapping("/spaces/{spaceId}")
    public ResponseEntity<DocSpaceResponse> getSpace(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId) {
        return ResponseEntity.ok(spaceService.getSpace(spaceId));
    }

    @PutMapping("/spaces/{spaceId}")
    public ResponseEntity<DocSpaceResponse> updateSpace(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @Valid @RequestBody DocSpaceRequest request) {
        return ResponseEntity.ok(spaceService.updateSpace(spaceId, request));
    }

    @DeleteMapping("/spaces/{spaceId}")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId) {
        spaceService.deleteSpace(spaceId);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAGES
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/spaces/{spaceId}/pages")
    public ResponseEntity<DocPageResponse> createPage(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @Valid @RequestBody DocPageRequest request) {
        return ResponseEntity.ok(pageService.createPage(spaceId, request));
    }

    @GetMapping("/spaces/{spaceId}/pages")
    public ResponseEntity<List<DocPageResponse>> getPageTree(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId) {
        return ResponseEntity.ok(pageService.getPageTree(spaceId));
    }

    @GetMapping("/spaces/{spaceId}/pages/{pageId}")
    public ResponseEntity<DocPageResponse> getPage(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId) {
        return ResponseEntity.ok(pageService.getPage(pageId));
    }

    @PutMapping("/spaces/{spaceId}/pages/{pageId}")
    public ResponseEntity<DocPageResponse> updatePage(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId,
            @Valid @RequestBody DocPageRequest request) {
        return ResponseEntity.ok(pageService.updatePage(pageId, request));
    }

    @DeleteMapping("/spaces/{spaceId}/pages/{pageId}")
    public ResponseEntity<Void> deletePage(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId) {
        pageService.deletePage(pageId);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/spaces/{spaceId}/pages/{pageId}/versions")
    public ResponseEntity<List<DocPageVersionResponse>> getVersions(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId) {
        return ResponseEntity.ok(pageService.getVersions(pageId));
    }

    @GetMapping("/spaces/{spaceId}/pages/{pageId}/versions/{versionNumber}")
    public ResponseEntity<DocPageVersionResponse> getVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId,
            @PathVariable int versionNumber) {
        return ResponseEntity.ok(pageService.getVersion(pageId, versionNumber));
    }

    @PostMapping("/spaces/{spaceId}/pages/{pageId}/versions/{versionNumber}/restore")
    public ResponseEntity<DocPageResponse> restoreVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId,
            @PathVariable int versionNumber) {
        return ResponseEntity.ok(pageService.restoreVersion(pageId, versionNumber));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/permissions")
    public ResponseEntity<DocPermissionResponse> grantPermission(
            @PathVariable UUID projectId,
            @Valid @RequestBody DocPermissionRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(permissionService.grantPermission(projectId, request, user));
    }

    @PostMapping("/permissions/delegate")
    public ResponseEntity<DocPermissionResponse> delegatePermission(
            @PathVariable UUID projectId,
            @Valid @RequestBody DocPermissionRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(permissionService.delegatePermission(projectId, request, user));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<DocPermissionResponse>> getPermissions(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) UUID pageId) {
        return ResponseEntity.ok(permissionService.getPermissions(spaceId, pageId));
    }

    @GetMapping("/permissions/targets")
    public ResponseEntity<List<DocPermissionTargetResponse>> searchPermissionTargets(
            @PathVariable UUID projectId,
            @RequestParam DocTargetType type,
            @RequestParam(required = false, defaultValue = "") String q) {
        User user = getCurrentUser();
        return ResponseEntity.ok(permissionService.searchTargets(projectId, type, q, user));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> revokePermission(
            @PathVariable UUID projectId,
            @PathVariable UUID permissionId) {
        User user = getCurrentUser();
        permissionService.revokePermission(permissionId, user);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/spaces/{spaceId}/pages/{pageId}/attachments")
    public ResponseEntity<DocAttachmentResponse> uploadAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(attachmentService.upload(pageId, file));
    }

    @GetMapping("/spaces/{spaceId}/pages/{pageId}/attachments")
    public ResponseEntity<List<DocAttachmentResponse>> getAttachments(
            @PathVariable UUID projectId,
            @PathVariable UUID spaceId,
            @PathVariable UUID pageId) {
        return ResponseEntity.ok(attachmentService.getAttachments(pageId));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID attachmentId) {
        InputStream stream = attachmentService.download(attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}

