package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_attachments", indexes = {
        @Index(name = "idx_attachment_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** Orijinal dosya adı (kullanıcının yüklediği) */
    @Column(nullable = false)
    private String fileName;

    /** MinIO object key (tam yol) — dosya indirme/silme için kullanılır */
    @Column(nullable = false)
    private String objectKey;

    /** Dosya boyutu (bytes) */
    @Column(nullable = false)
    private Long fileSize;

    /** MIME type (ör: image/png, application/pdf) */
    private String mimeType;

    /** Yükleyen kullanıcı email */
    @Column(nullable = false)
    private String uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

