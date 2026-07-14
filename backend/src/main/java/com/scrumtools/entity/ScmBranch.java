package com.scrumtools.entity;

import com.scrumtools.entity.enums.ScmBranchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task'a bağlı git branch'i. Uygulamadan açılabilir (createdViaApp=true)
 * ya da adındaki TEAMCODE-N anahtarıyla webhook'tan keşfedilebilir.
 * Branch her zaman bir taska bağlı kaydedilir — anahtar addan silinse bile
 * görünürlük kaybolmaz.
 */
@Entity
@Table(name = "scm_branches",
        uniqueConstraints = @UniqueConstraint(name = "uq_scm_branch",
                columnNames = {"repository_id", "task_id", "name"}),
        indexes = {
                @Index(name = "idx_scm_branch_task", columnList = "task_id"),
                @Index(name = "idx_scm_branch_repo", columnList = "repository_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScmBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private ScmRepository repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** Örn: feature/SCRM-123-login-hatasi */
    @Column(nullable = false)
    private String name;

    @Column
    private String webUrl;

    /** true: uygulamadan açıldı; false: webhook ile keşfedildi */
    @Builder.Default
    @Column(nullable = false)
    private boolean createdViaApp = false;

    /** Uygulamadan açıldıysa açan kullanıcı (email) */
    @Column
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScmBranchStatus status = ScmBranchStatus.ACTIVE;

    @Column
    private String lastCommitSha;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
