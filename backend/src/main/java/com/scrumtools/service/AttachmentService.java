package com.scrumtools.service;

import com.scrumtools.dto.AttachmentResponse;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.TaskAttachment;
import com.scrumtools.repository.TaskAttachmentRepository;
import com.scrumtools.repository.TaskRepository;
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
public class AttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final StorageService storageService;

    /** Maksimum tek dosya boyutu: 20MB */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    /** Task başına maksimum dosya sayısı */
    private static final int MAX_ATTACHMENTS_PER_TASK = 20;

    // ─── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public AttachmentResponse upload(UUID teamId, UUID taskId, MultipartFile file) {
        // Validasyon
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya boyutu 20MB'ı aşamaz");
        }
        if (attachmentRepository.countByTaskId(taskId) >= MAX_ATTACHMENTS_PER_TASK) {
            throw new IllegalArgumentException("Bir task'a en fazla " + MAX_ATTACHMENTS_PER_TASK + " dosya eklenebilir");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task bulunamadı"));
        if (!task.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Task bu takıma ait değil");
        }

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // MinIO'ya yükle
        String objectKey = storageService.upload(teamId, taskId, file);

        // DB'ye kaydet
        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(userEmail)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Dosya eklendi: {} → task {}", attachment.getFileName(), task.getCustomId());

        String downloadUrl = storageService.getPresignedUrl(objectKey, 60);
        return AttachmentResponse.from(attachment, downloadUrl);
    }

    // ─── List ──────────────────────────────────────────────────────────────────

    public List<AttachmentResponse> getAttachments(UUID teamId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task bulunamadı"));
        if (!task.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Task bu takıma ait değil");
        }

        return attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(a -> {
                    String downloadUrl = storageService.getPresignedUrl(a.getObjectKey(), 60);
                    return AttachmentResponse.from(a, downloadUrl);
                })
                .collect(Collectors.toList());
    }

    // ─── Download ──────────────────────────────────────────────────────────────

    public InputStream download(UUID attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));
        return storageService.download(attachment.getObjectKey());
    }

    public TaskAttachment getAttachmentEntity(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID teamId, UUID taskId, UUID attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));
        if (!attachment.getTask().getId().equals(taskId)) {
            throw new RuntimeException("Dosya bu task'a ait değil");
        }
        if (!attachment.getTask().getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Task bu takıma ait değil");
        }

        // MinIO'dan sil
        storageService.delete(attachment.getObjectKey());

        // DB'den sil
        attachmentRepository.delete(attachment);
        log.info("Dosya silindi: {}", attachment.getFileName());
    }
}

