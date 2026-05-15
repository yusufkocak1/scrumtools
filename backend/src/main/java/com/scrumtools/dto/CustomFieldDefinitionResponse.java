package com.scrumtools.dto;

import com.scrumtools.entity.CustomFieldDefinition;
import com.scrumtools.entity.enums.CustomFieldType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id,
        String name,
        String fieldKey,
        CustomFieldType fieldType,
        Map<String, Object> options,
        Boolean isRequired,
        Boolean isVisible,
        List<String> issueTypes,
        String defaultValue,
        Integer position
) {
    public static CustomFieldDefinitionResponse from(CustomFieldDefinition d) {
        return new CustomFieldDefinitionResponse(
                d.getId(), d.getName(), d.getFieldKey(), d.getFieldType(),
                d.getOptions(), d.getIsRequired(), d.getIsVisible(),
                d.getIssueTypes(), d.getDefaultValue(), d.getPosition()
        );
    }
}

