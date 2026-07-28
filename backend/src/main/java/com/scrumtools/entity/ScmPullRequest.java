package com.scrumtools.entity;

import com.scrumtools.entity.enums.ScmPullRequestState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task'a bağlı pull request (GitLab'da merge request). Uygulamadan, göreve
 * bağlı bir branch üzerinden açılır — bu yüzden branch bağlantısı tutulur.
 * Durum sağlayıcıdan yenilenerek (refresh) güncellenir; kayıt silinmez.
 */
@Entity
@Table(name = "scm_pull_requests",
        uniqueConstraints = @UniqueConstraint(name = "uq_scm_pull_request",
                columnNames = {"repository_id", "task_id", "externalId"}),
        indexes = {
                @Index(name = "idx_scm_pr_task", columnList = "task_id"),
                @Index(name = "idx_scm_pr_repo", columnList = "repository_id"),
                @Index(name = "idx_scm_pr_branch", columnList = "branch_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScmPullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private ScmRepository repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** PR'ın açıldığı, göreve bağlı branch — branch kaydı silinirse null'a düşer */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private ScmBranch branch;

    /** Sağlayıcıdaki numara: GitHub PR number, GitLab MR iid */
    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private String sourceBranch;

    @Column(nullable = false)
    private String targetBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScmPullRequestState state = ScmPullRequestState.OPEN;

    @Column
    private String webUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean draft = false;

    /** true: uygulamadan açıldı (şu an tek yol; ileride webhook keşfi için ayrılmıştır) */
    @Builder.Default
    @Column(nullable = false)
    private boolean createdViaApp = true;

    @Column
    private String createdBy;

    @Column
    private LocalDateTime mergedAt;

    /** Sağlayıcıdan en son ne zaman durum çekildi */
    @Column
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
