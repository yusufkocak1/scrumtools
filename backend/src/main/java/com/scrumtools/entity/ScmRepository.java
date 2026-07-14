package com.scrumtools.entity;

import com.scrumtools.entity.enums.WebhookStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeye eşlenmiş SCM reposu. Bir repo aynı anda tek projeye eşlenebilir
 * (unique connection_id + external_id) — çoklu eşleme commit-task
 * çözümlemesini belirsizleştirdiği için Faz 1'de bilinçli olarak engellendi.
 */
@Entity
@Table(name = "scm_repositories",
        uniqueConstraints = @UniqueConstraint(name = "uq_scm_repo_conn_external",
                columnNames = {"connection_id", "externalId"}),
        indexes = {
                @Index(name = "idx_scm_repo_project", columnList = "project_id"),
                @Index(name = "idx_scm_repo_connection", columnList = "connection_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScmRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private ScmConnection connection;

    /** Eşlendiği proje — task'lar Team → Project zinciriyle bu repolara ulaşır */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Sağlayıcıdaki repo kimliği: GitHub "owner/name", GitLab numeric id */
    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    /** "kocak/scrumtools" gibi tam ad */
    @Column
    private String fullName;

    @Column
    private String defaultBranch;

    /** Tarayıcı linki */
    @Column
    private String webUrl;

    /** Sağlayıcıdaki hook id'si — eşleme kaldırılırken webhook silmek için */
    @Column
    private String webhookExternalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WebhookStatus webhookStatus = WebhookStatus.NONE;

    /** Poller için işaretçi: en son hangi ana kadar commit tarandı */
    @Column
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
