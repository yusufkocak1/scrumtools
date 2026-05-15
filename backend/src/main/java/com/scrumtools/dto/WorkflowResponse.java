package com.scrumtools.dto;

import com.scrumtools.entity.Workflow;
import com.scrumtools.entity.WorkflowStatus;
import com.scrumtools.entity.WorkflowTransition;
import com.scrumtools.entity.enums.StatusCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String description,
        Boolean isDefault,
        List<String> issueTypes,
        List<StatusDto> statuses,
        List<TransitionDto> transitions,
        LocalDateTime createdAt
) {
    public record StatusDto(
            UUID id,
            String name,
            StatusCategory category,
            String color,
            String icon,
            Integer position,
            Boolean isInitial,
            Boolean isFinal,
            String description
    ) {
        public static StatusDto from(WorkflowStatus s) {
            return new StatusDto(s.getId(), s.getName(), s.getCategory(), s.getColor(),
                    s.getIcon(), s.getPosition(), s.getIsInitial(), s.getIsFinal(), s.getDescription());
        }
    }

    public record TransitionDto(
            UUID id,
            String name,
            UUID fromStatusId,
            String fromStatusName,
            UUID toStatusId,
            String toStatusName,
            List<UUID> allowedRoles,
            Map<String, Object> conditions,
            Map<String, Object> actions,
            Integer position
    ) {
        public static TransitionDto from(WorkflowTransition t) {
            return new TransitionDto(t.getId(), t.getName(),
                    t.getFromStatus() != null ? t.getFromStatus().getId() : null,
                    t.getFromStatus() != null ? t.getFromStatus().getName() : null,
                    t.getToStatus().getId(), t.getToStatus().getName(),
                    t.getAllowedRoles(), t.getConditions(), t.getActions(), t.getPosition());
        }
    }

    public static WorkflowResponse from(Workflow w) {
        return new WorkflowResponse(
                w.getId(), w.getName(), w.getDescription(), w.getIsDefault(),
                w.getIssueTypes(),
                w.getStatuses().stream().map(StatusDto::from).toList(),
                w.getTransitions().stream().map(TransitionDto::from).toList(),
                w.getCreatedAt()
        );
    }
}

