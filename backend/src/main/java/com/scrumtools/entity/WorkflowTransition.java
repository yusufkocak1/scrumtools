package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow'daki iki durum arasındaki geçiş tanımı.
 * allowedRoles boş ise herkes geçiş yapabilir.
 */
@Entity
@Table(name = "workflow_transitions", indexes = {
        @Index(name = "idx_wtrans_workflow", columnList = "workflow_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id")
    private WorkflowStatus fromStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", nullable = false)
    private WorkflowStatus toStatus;

    /**
     * Bu geçişi yapabilecek rol id'leri. Boş = herkes yapabilir.
     */
    @ElementCollection
    @CollectionTable(name = "workflow_transition_roles", joinColumns = @JoinColumn(name = "transition_id"))
    @Column(name = "role_id")
    @Builder.Default
    private List<UUID> allowedRoles = new ArrayList<>();

    /**
     * Geçiş koşulları JSON:
     * { requireAssignee, requireDescription, requireComment, requireAllSubtasksDone }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> conditions;

    /**
     * Geçiş aksiyonları JSON:
     * { autoAssignTo, sendNotification, addLabel, setField }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> actions;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;
}

