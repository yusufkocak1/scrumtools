package com.scrumtools.dto;

import com.scrumtools.entity.TaskHistory;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskHistoryResponse(
        UUID id,
        String field,
        String oldValue,
        String newValue,
        String changedBy,
        LocalDateTime changedAt
) {
    public static TaskHistoryResponse from(TaskHistory h) {
        return new TaskHistoryResponse(
                h.getId(), h.getField(), h.getOldValue(),
                h.getNewValue(), h.getChangedBy(), h.getChangedAt()
        );
    }
}

