package com.scrumtools.service;

import com.scrumtools.dto.DocAttachmentResponse;
import com.scrumtools.entity.DocPage;
import com.scrumtools.entity.DocPageAttachment;
import com.scrumtools.entity.User;
import com.scrumtools.repository.DocPageAttachmentRepository;
import com.scrumtools.repository.DocPageRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocAttachmentService {

    private final DocPageAttachmentRepository attachmentRepository;
    private final DocPageRepository pageRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final DocPermissionService permissionService;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int MAX_ATTACHMENTS_PER_PAGE = 50;

    @Transactional
    public DocAttachmentResponse upload(UUID pageId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya boyutu 20MB'ı aşamaz");
        }

        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(page.getSpace(), page, user);

        if (attachmentRepository.countByPageId(pageId) >= MAX_ATTACHMENTS_PER_PAGE) {
            throw new IllegalArgumentException("Bir sayfaya en fazla " + MAX_ATTACHMENTS_PER_PAGE + " dosya eklenebilir");
        }

        // MinIO'ya yükle
        String pathPrefix = String.format("docs/spaces/%s/pages/%s", page.getSpace().getId(), pageId);
        String objectKey = storageService.upload(pathPrefix, file);

        // DB'ye kaydet
        DocPageAttachment attachment = DocPageAttachment.builder()
                .page(page)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(user)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Doc attachment yüklendi: {} → page {}", attachment.getFileName(), pageId);

        String downloadUrl = storageService.getPresignedUrl(objectKey, 60);
        return DocAttachmentResponse.from(attachment, downloadUrl);
    }

    public List<DocAttachmentResponse> getAttachments(UUID pageId) {
        DocPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Sayfa bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(page.getSpace(), page, user);

        return attachmentRepository.findByPageIdOrderByCreatedAtDesc(pageId).stream()
                .map(a -> {
                    String url = storageService.getPresignedUrl(a.getObjectKey(), 60);
                    return DocAttachmentResponse.from(a, url);
                })
                .collect(Collectors.toList());
    }

    public InputStream download(UUID attachmentId) {
        DocPageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(attachment.getPage().getSpace(), attachment.getPage(), user);

        return storageService.download(attachment.getObjectKey());
    }

    @Transactional
    public void delete(UUID attachmentId) {
        DocPageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkWriteAccess(attachment.getPage().getSpace(), attachment.getPage(), user);

        storageService.delete(attachment.getObjectKey());
        attachmentRepository.delete(attachment);
        log.info("Doc attachment silindi: {}", attachment.getFileName());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}

