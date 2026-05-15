package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Task entity — core fields sabit, customFields JSONB ile yatay genişleyebilir.
 *
 * Jira alternatifi vizyon doğrultusunda:
 *  - Standart alanlar (title, description, status vb.) dedicated column olarak tutulur — sorgulama/indeksleme açısından verimli.
 *  - Ekip/proje bazlı özel alanlar (örn. "sprint story", "environment", "fix version") customFields (JSONB) içinde tutulur.
 *  - İleride TaskFieldDefinition entity'si ile per-team alan tanımları, dashboard filtresi ve arama özellikleri eklenebilir.
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_team", columnList = "team_id"),
        @Index(name = "idx_task_custom_id", columnList = "customId"),
        @Index(name = "idx_task_sprint", columnList = "sprint_id"),
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_parent", columnList = "parent_task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    // ─── Alt Görev (Subtask) Desteği ─────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Task> subtasks = new ArrayList<>();

    /**
     * TEAMCODE-N formatında benzersiz iş öğesi kimliği (backend üretir, race condition'a karşı DB sequence ile korunur).
     */
    @Column(nullable = false)
    private String customId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * To Do | In Progress | Done | Cancelled
     */
    @Builder.Default
    @Column(nullable = false)
    private String status = "To Do";

    /** task | story | bug | epic */
    @Builder.Default
    private String issueType = "task";

    /** Low | Medium | High | Critical */
    @Builder.Default
    private String priority = "Medium";

    /** Issue'yu açan kişi (email) */
    private String reporter;

    /** Assignee email */
    private String assignee;
    private String developer;
    private String analyst;
    private String tester;

    private Integer storyPoints;

    // ─── Tarihler ─────────────────────────────────────────────────────────────
    private LocalDate dueDate;
    private LocalDate startDate;

    // ─── Zaman Takibi ─────────────────────────────────────────────────────────
    private Double estimatedHours;
    private Double loggedHours;

    // ─── Ek Alanlar ───────────────────────────────────────────────────────────
    /** Production | Staging | Dev vb. */
    private String environment;

    /** Fixed | Won't Fix | Duplicate | Cannot Reproduce */
    private String resolution;

    private LocalDateTime resolvedAt;

    /** Board/backlog içindeki sıralama pozisyonu */
    @Builder.Default
    private Integer position = 0;

    @ElementCollection
    @CollectionTable(name = "task_labels", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "label")
    @Builder.Default
    private List<String> labels = new ArrayList<>();

    /** Takip eden kullanıcıların email listesi */
    @ElementCollection
    @CollectionTable(name = "task_watchers", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "watcher_email")
    @Builder.Default
    private List<String> watchers = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskAttachment> attachments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
