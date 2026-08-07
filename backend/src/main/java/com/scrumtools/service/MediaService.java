package com.scrumtools.service;

import com.scrumtools.repository.DocPageAttachmentRepository;
import com.scrumtools.repository.TaskAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * İmzalı medya bağlantılarının arkasındaki dosyayı açar.
 *
 * Yetki kontrolü {@link MediaLinkService} imzasıyla yapılır; imza geçerli değilse
 * dosyaya hiç bakılmaz (var/yok bilgisi de sızmasın diye çağıran 404 döner).
 */
@Service
@RequiredArgsConstructor
public class MediaService {

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final DocPageAttachmentRepository docAttachmentRepository;
    private final StorageService storageService;
    private final MediaLinkService mediaLinkService;

    /** Servis edilecek dosyanın içeriği ve HTTP başlıkları için gereken üst bilgisi. */
    public record MediaObject(String fileName, String mimeType, long fileSize, InputStream stream) {
    }

    public Optional<MediaObject> openTaskAttachment(UUID attachmentId, String signature) {
        if (!mediaLinkService.verify(MediaLinkService.TASK_ATTACHMENT, attachmentId, signature)) {
            return Optional.empty();
        }
        return taskAttachmentRepository.findById(attachmentId)
                .map(a -> new MediaObject(
                        a.getFileName(),
                        a.getMimeType(),
                        a.getFileSize() != null ? a.getFileSize() : 0L,
                        storageService.download(a.getObjectKey())));
    }

    public Optional<MediaObject> openDocAttachment(UUID attachmentId, String signature) {
        if (!mediaLinkService.verify(MediaLinkService.DOC_ATTACHMENT, attachmentId, signature)) {
            return Optional.empty();
        }
        return docAttachmentRepository.findById(attachmentId)
                .map(a -> new MediaObject(
                        a.getFileName(),
                        a.getMimeType(),
                        a.getFileSize() != null ? a.getFileSize() : 0L,
                        storageService.download(a.getObjectKey())));
    }
}
