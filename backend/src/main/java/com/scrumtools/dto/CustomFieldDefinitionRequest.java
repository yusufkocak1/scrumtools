package com.scrumtools.dto;

import com.scrumtools.entity.enums.CustomFieldType;

import java.util.List;
import java.util.Map;

public record CustomFieldDefinitionRequest(
        String name,
        String fieldKey,
        CustomFieldType fieldType,
        Map<String, Object> options,
        Boolean isRequired,
        Boolean isVisible,
        List<String> issueTypes,
        String defaultValue,
        Integer position
) {}

