package com.scrumtools.dto;

import com.scrumtools.entity.CiBuild;

import java.time.LocalDateTime;
import java.util.UUID;

/** Build kaydı yanıtı — task/release panellerinde ve proje geneli tarihçede kullanılır. */
public record CiBuildResponse(
        UUID id,
        UUID jobMappingId,
        String jobDisplayName,
        String jobType,
        String environment,
        String contextType,
        UUID taskId,
        String taskCustomId,
        String taskTitle,
        UUID releaseId,
        String releaseName,
        Integer buildNumber,
        String buildUrl,
        String status,
        String statusMessage,
        String branch,
        String triggeredBy,
        LocalDateTime triggeredAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs
) {
    public static CiBuildResponse from(CiBuild b) {
        var mapping = b.getJobMapping();
        return new CiBuildResponse(
                b.getId(),
                mapping.getId(),
                mapping.getDisplayName(),
                mapping.getJobType().name(),
                mapping.getEnvironment().name(),
                b.getContextType().name(),
                b.getTaskId(),
                b.getTaskCustomId(),
                b.getTaskTitle(),
                b.getReleaseId(),
                b.getReleaseName(),
                b.getBuildNumber(),
                b.getBuildUrl(),
                b.getStatus().name(),
                b.getStatusMessage(),
                b.getBranch(),
                b.getTriggeredBy(),
                b.getTriggeredAt(),
                b.getStartedAt(),
                b.getFinishedAt(),
                b.getDurationMs()
        );
    }
}
