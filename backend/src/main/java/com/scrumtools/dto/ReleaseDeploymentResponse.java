package com.scrumtools.dto;

import com.scrumtools.entity.ReleaseDeployment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReleaseDeploymentResponse {

    private String id;
    private String releaseId;
    private String releaseName;
    private String taskId;
    private String taskCustomId;
    private String taskTitle;
    private String taskStatusAtRelease;
    private String releasedBy;
    private LocalDateTime releasedAt;

    public static ReleaseDeploymentResponse from(ReleaseDeployment deployment) {
        return ReleaseDeploymentResponse.builder()
                .id(deployment.getId().toString())
                .releaseId(deployment.getRelease().getId().toString())
                .releaseName(deployment.getRelease().getName())
                .taskId(deployment.getTaskId().toString())
                .taskCustomId(deployment.getTaskCustomId())
                .taskTitle(deployment.getTaskTitle())
                .taskStatusAtRelease(deployment.getTaskStatusAtRelease())
                .releasedBy(deployment.getReleasedBy())
                .releasedAt(deployment.getReleasedAt())
                .build();
    }
}
