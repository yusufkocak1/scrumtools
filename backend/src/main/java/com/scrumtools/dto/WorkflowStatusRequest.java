package com.scrumtools.dto;

import com.scrumtools.entity.enums.StatusCategory;

public record WorkflowStatusRequest(
        String name,
        StatusCategory category,
        String color,
        String icon,
        Integer position,
        Boolean isInitial,
        Boolean isFinal,
        String description
) {}

