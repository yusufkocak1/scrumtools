package com.scrumtools.controller;

import com.scrumtools.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * İçeriğe gömülü medyayı (görsel/dosya) kalıcı, imzalı bağlantı üzerinden servis eder.
 *
 * SecurityConfig'te /api/media/** permitAll'dır: tarayıcının &lt;img src&gt; isteği
 * Authorization header'ı taşıyamaz. Yetkilendirme URL'deki HMAC imzasıdır
 * ({@link com.scrumtools.service.MediaLinkService}) — imza tutmuyorsa 404 döner.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /** GET /api/media/task-attachments/{attachmentId}?s={imza} */
    @GetMapping("/task-attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> taskAttachment(
            @PathVariable UUID attachmentId,
            @RequestParam(name = "s", required = false) String signature,
            @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        return respond(mediaService.openTaskAttachment(attachmentId, signature), download);
    }

    /** GET /api/media/doc-attachments/{attachmentId}?s={imza} */
    @GetMapping("/doc-attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> docAttachment(
            @PathVariable UUID attachmentId,
            @RequestParam(name = "s", required = false) String signature,
            @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        return respond(mediaService.openDocAttachment(attachmentId, signature), download);
    }

    private ResponseEntity<InputStreamResource> respond(Optional<MediaService.MediaObject> media, boolean download) {
        if (media.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MediaService.MediaObject object = media.get();

        ContentDisposition disposition = (download
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(object.fileName(), StandardCharsets.UTF_8)
                .build();

        // Bağlantı ek kaydının kimliğine bağlı ve dosya değişmiyor — uzun süre cache'lenebilir.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate().immutable())
                .header("Content-Disposition", disposition.toString())
                .contentType(MediaType.parseMediaType(
                        object.mimeType() != null ? object.mimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(object.fileSize())
                .body(new InputStreamResource(object.stream()));
    }
}
