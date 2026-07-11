package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dağıtım tarihçesi — hangi iş hangi release ile ne zaman yayınlandı.
 * RELEASED geçişinde bir kez yazılır, asla güncellenmez.
 * taskId bilinçli olarak JPA FK değil düz kolon: task silinse veya başka
 * release'e taşınsa bile tarihçe kaydı değişmeden korunur (denormalize snapshot).
 */
@Entity
@Table(name = "release_deployments", indexes = {
        @Index(name = "idx_release_deployment_release", columnList = "release_id"),
        @Index(name = "idx_release_deployment_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    /** Yayın anındaki task snapshot alanları */
    private String taskCustomId;
    private String taskTitle;
    private String taskStatusAtRelease;

    /** Release'i yayınlayan kullanıcı email */
    @Column(nullable = false)
    private String releasedBy;

    @CreationTimestamp
    @Column(name = "released_at", updatable = false)
    private LocalDateTime releasedAt;
}
