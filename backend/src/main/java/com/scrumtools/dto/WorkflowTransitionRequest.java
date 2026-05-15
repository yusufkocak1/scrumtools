package com.scrumtools.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowTransitionRequest(
        String name,
        UUID fromStatusId,
        UUID toStatusId,
        List<UUID> allowedRoles,
        Map<String, Object> conditions,
        Map<String, Object> actions,
        Integer position
) {}

