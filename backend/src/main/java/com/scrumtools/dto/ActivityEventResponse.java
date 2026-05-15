package com.scrumtools.dto;

import com.scrumtools.entity.ActivityEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ActivityEventResponse {
    private String id;
    private String actorEmail;
    private String action;
    private String entityType;
    private String entityId;
    private String teamId;
    private Map<String, Object> details;
    private LocalDateTime createdAt;

    public static ActivityEventResponse from(ActivityEvent e) {
        return ActivityEventResponse.builder()
                .id(e.getId().toString())
                .actorEmail(e.getActorEmail())
                .action(e.getAction().name())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .teamId(e.getTeam() != null ? e.getTeam().getId().toString() : null)
                .details(e.getDetails())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

