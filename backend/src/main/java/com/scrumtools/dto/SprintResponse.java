package com.scrumtools.dto;

import com.scrumtools.entity.Sprint;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SprintResponse {
    private String id;
    private String teamId;
    private String name;
    private String goal;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static SprintResponse from(Sprint sprint) {
        return SprintResponse.builder()
                .id(sprint.getId().toString())
                .teamId(sprint.getTeam().getId().toString())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .status(sprint.getStatus())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .createdAt(sprint.getCreatedAt())
                .build();
    }
}
