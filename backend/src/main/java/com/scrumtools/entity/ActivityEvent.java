package com.scrumtools.entity;

import com.scrumtools.entity.enums.ActivityAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "activity_events", indexes = {
        @Index(name = "idx_activity_team",    columnList = "team_id"),
        @Index(name = "idx_activity_actor",   columnList = "actor_email"),
        @Index(name = "idx_activity_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Aksiyonu yapan kullanıcının e-postası */
    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityAction action;

    /** Etkilenen entity tipi: "task", "sprint", "board" vb. */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    /** Etkilenen entity'nin ID'si */
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    /** Aktivitenin ait olduğu takım — güvenli filtreleme için */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /** Ek detaylar: eski/yeni değer, açıklama vb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

