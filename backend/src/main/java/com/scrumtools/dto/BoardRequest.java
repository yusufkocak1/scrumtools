package com.scrumtools.dto;

import com.scrumtools.entity.enums.BoardType;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class BoardRequest {
    private String name;
    private BoardType boardType;
    private UUID projectId;
    private Map<String, Object> columnConfig;
    private Map<String, Object> swimlaneConfig;
    private Map<String, Object> cardConfig;
    private Boolean isDefault;
}
