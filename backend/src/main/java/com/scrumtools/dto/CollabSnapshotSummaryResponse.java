package com.scrumtools.dto;

import com.scrumtools.entity.CollabSnapshot;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Zaman çizelgesi satırı. Metnin kendisi taşınmaz — geçmiş listesi onlarca
 * kayıt çeker ve her biri dokümanın tamamı kadar büyük olabilir.
 */
public record CollabSnapshotSummaryResponse(
        UUID id,
        Long seq,
        Integer participantCount,
        String createdByName,
        LocalDateTime createdAt,
        int length
) {
    public static CollabSnapshotSummaryResponse from(CollabSnapshot s) {
        return new CollabSnapshotSummaryResponse(
                s.getId(),
                s.getSeq(),
                s.getParticipantCount(),
                s.getCreatedBy() != null ? s.getCreatedBy().getName() : null,
                s.getCreatedAt(),
                s.getSnapshotText() != null ? s.getSnapshotText().length() : 0
        );
    }
}
