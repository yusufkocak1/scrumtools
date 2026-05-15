package com.scrumtools.entity;

import com.scrumtools.entity.enums.StatusCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bir workflow'a ait tek bir durum (state) tanımı.
 */
@Entity
@Table(name = "workflow_statuses", indexes = {
        @Index(name = "idx_wstatus_workflow", columnList = "workflow_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String name;

    /**
     * Raporlama ve board gruplama için kategori (TO_DO / IN_PROGRESS / DONE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusCategory category = StatusCategory.TO_DO;

    @Column
    @Builder.Default
    private String color = "#6B7280";

    /** Emoji veya icon adı */
    @Column
    private String icon;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    /** Yeni issue açıldığında bu status mu atanacak */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isInitial = false;

    /** Bu status'a geçildiğinde iş tamamlandı sayılır */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFinal = false;

    @Column(columnDefinition = "TEXT")
    private String description;
}

