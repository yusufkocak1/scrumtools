package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bir projeye ait özelleştirilebilir iş akışı tanımı.
 * Her proje birden fazla workflow'a sahip olabilir (Scrum Flow, Bug Fix Flow vb.)
 */
@Entity
@Table(name = "workflows", indexes = {
        @Index(name = "idx_workflow_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // team bazlı workflow desteği (project olmayan takımlar için)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Bu workflow'u hangi issue type'lar kullanacak (ör: ["task","story","bug"])
     * Boş liste = tüm tipler
     */
    @ElementCollection
    @CollectionTable(name = "workflow_issue_types", joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "issue_type")
    @Builder.Default
    private List<String> issueTypes = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<WorkflowStatus> statuses = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowTransition> transitions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

