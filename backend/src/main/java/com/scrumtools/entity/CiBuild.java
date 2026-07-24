package com.scrumtools.entity;

import com.scrumtools.entity.enums.CiBuildContext;
import com.scrumtools.entity.enums.CiBuildStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tetiklenen her build'in kaydı ve son bilinen durumu.
 * taskId/releaseId bilinçli olarak JPA FK değil düz kolon (ReleaseDeployment ile
 * aynı gerekçe): task veya release silinse de build tarihçesi bozulmadan kalır;
 * snapshot alanları (taskCustomId, releaseName) o anki bağlamı korur.
 */
@Entity
@Table(name = "ci_builds", indexes = {
        @Index(name = "idx_ci_build_mapping", columnList = "job_mapping_id"),
        @Index(name = "idx_ci_build_task", columnList = "task_id"),
        @Index(name = "idx_ci_build_release", columnList = "release_id"),
        @Index(name = "idx_ci_build_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_mapping_id", nullable = false)
    private CiJobMapping jobMapping;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CiBuildContext contextType;

    @Column(name = "task_id")
    private UUID taskId;

    /** Tetikleme anındaki task snapshot'ı */
    @Column
    private String taskCustomId;

    @Column
    private String taskTitle;

    @Column(name = "release_id")
    private UUID releaseId;

    /** Tetikleme anındaki release adı (sürüm no) */
    @Column
    private String releaseName;

    /**
     * buildWithParameters yanıtındaki Location başlığı — build numarası
     * kuyruktan çözülene kadar takip bunun üzerinden yapılır.
     */
    @Column(columnDefinition = "TEXT")
    private String queueItemUrl;

    /** Kuyruk çözülünce dolar */
    @Column
    private Integer buildNumber;

    /** Jenkins'teki build sayfasına derin link */
    @Column(columnDefinition = "TEXT")
    private String buildUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CiBuildStatus status = CiBuildStatus.QUEUED;

    /** Jenkins'e gönderilen çözümlenmiş parametreler — denetim izi (JSON) */
    @Column(columnDefinition = "TEXT")
    private String parametersJson;

    /** Deploy edilen branch (varsa) */
    @Column
    private String branch;

    /** Terminal durumda kullanıcıya gösterilecek kısa açıklama (hata sebebi vb.) */
    @Column(columnDefinition = "TEXT")
    private String statusMessage;

    @Column(nullable = false)
    private String triggeredBy;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "triggered_at", updatable = false)
    private LocalDateTime triggeredAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
