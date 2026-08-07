package com.scrumtools.entity;

import com.scrumtools.entity.enums.MacroRunStatus;
import com.scrumtools.entity.enums.MacroTriggerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tek bir makro çalıştırması — denetim kaydı (COLLAB_WORKSPACE_PLAN.md §9.2).
 *
 * <p>Reddedilen çalıştırmalar da yazılır: "kim neyi çalıştırmaya kalktı" sorusu,
 * "ne çalıştı" sorusundan daha önemli olabilir.
 */
@Entity
@Table(name = "collab_macro_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabMacroRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "macro_id", nullable = false)
    private CollabMacro macro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private MacroTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MacroRunStatus status;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** Makronun {@code console.log} çıktısı — istemciden gelir, kırpılmış. */
    @Column(columnDefinition = "TEXT")
    private String log;

    @Column(columnDefinition = "TEXT")
    private String error;

    /** Dokümana yazan makrolar aktivite akışına düşer; salt okuyanlar düşmez. */
    @Column(name = "wrote_document", nullable = false)
    @Builder.Default
    private Boolean wroteDocument = false;
}
