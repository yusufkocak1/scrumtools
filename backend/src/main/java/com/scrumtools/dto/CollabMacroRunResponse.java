package com.scrumtools.dto;

import com.scrumtools.entity.CollabMacroRun;
import com.scrumtools.entity.enums.MacroRunStatus;
import com.scrumtools.entity.enums.MacroTriggerType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Makro çalıştırma günlüğü kaydı (COLLAB_WORKSPACE_PLAN.md §9.2). */
public record CollabMacroRunResponse(
        UUID id,
        UUID macroId,
        String macroName,
        String triggeredByName,
        MacroTriggerType triggerType,
        MacroRunStatus status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        String log,
        String error,
        boolean wroteDocument
) {
    public static CollabMacroRunResponse from(CollabMacroRun run) {
        return new CollabMacroRunResponse(
                run.getId(),
                run.getMacro().getId(),
                run.getMacro().getName(),
                run.getTriggeredBy() != null ? run.getTriggeredBy().getName() : null,
                run.getTriggerType(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getDurationMs(),
                run.getLog(),
                run.getError(),
                Boolean.TRUE.equals(run.getWroteDocument()));
    }
}

