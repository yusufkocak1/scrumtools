package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mesajında task anahtarı (TEAMCODE-N) geçen commit'ler.
 * Eşleşmeyen commit'ler bilinçli olarak KAYDEDILMEZ (tablo şişmesi önlenir).
 * Unique (repository_id, sha) kısıtı webhook idempotency'sini sağlar —
 * aynı push tekrar gelirse commit ikinci kez işlenmez.
 */
@Entity
@Table(name = "scm_commits",
        uniqueConstraints = @UniqueConstraint(name = "uq_scm_commit",
                columnNames = {"repository_id", "sha"}),
        indexes = @Index(name = "idx_scm_commit_repo", columnList = "repository_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScmCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private ScmRepository repository;

    @Column(nullable = false, length = 64)
    private String sha;

    /** Commit mesajının ilk satırı, 500 karaktere kırpılmış */
    @Column(length = 500)
    private String shortMessage;

    @Column
    private String authorName;

    @Column
    private String authorEmail;

    @Column
    private LocalDateTime authoredAt;

    @Column
    private String webUrl;

    /** Push edildiği ref (webhook'tan); poller keşfinde null olabilir */
    @Column
    private String branchHint;

    /** Bir commit mesajında birden çok task anahtarı geçebilir — hepsi bağlanır */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "scm_commit_tasks",
            joinColumns = @JoinColumn(name = "commit_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id"))
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
