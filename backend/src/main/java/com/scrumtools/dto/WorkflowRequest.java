package com.scrumtools.dto;

import java.util.List;

public record WorkflowRequest(
        String name,
        String description,
        Boolean isDefault,
        List<String> issueTypes
) {}

