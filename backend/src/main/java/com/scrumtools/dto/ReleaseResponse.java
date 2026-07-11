package com.scrumtools.dto;

import com.scrumtools.entity.Release;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReleaseResponse {

    private String id;
    private String projectId;
    private String name;
    private String description;
    private String status;
    private String releaseManagerEmail;
    private String releaseManagerName;
    private LocalDate freezeDate;
    private LocalDate plannedReleaseDate;
    private LocalDateTime actualReleaseDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private long taskCount;
    private long doneTaskCount;

    public static ReleaseResponse from(Release release, long taskCount, long doneTaskCount) {
        return ReleaseResponse.builder()
                .id(release.getId().toString())
                .projectId(release.getProject().getId().toString())
                .name(release.getName())
                .description(release.getDescription())
                .status(release.getStatus().name())
                .releaseManagerEmail(release.getReleaseManager() != null ? release.getReleaseManager().getEmail() : null)
                .releaseManagerName(release.getReleaseManager() != null ? release.getReleaseManager().getName() : null)
                .freezeDate(release.getFreezeDate())
                .plannedReleaseDate(release.getPlannedReleaseDate())
                .actualReleaseDate(release.getActualReleaseDate())
                .createdBy(release.getCreatedBy())
                .createdAt(release.getCreatedAt())
                .taskCount(taskCount)
                .doneTaskCount(doneTaskCount)
                .build();
    }
}
