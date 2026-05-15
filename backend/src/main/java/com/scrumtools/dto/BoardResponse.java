package com.scrumtools.dto;

import com.scrumtools.entity.Board;
import com.scrumtools.entity.enums.BoardType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class BoardResponse {
    private UUID id;
    private UUID teamId;
    private UUID projectId;
    private String name;
    private BoardType boardType;
    private Map<String, Object> columnConfig;
    private Map<String, Object> swimlaneConfig;
    private Map<String, Object> cardConfig;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardResponse from(Board b) {
        return BoardResponse.builder()
                .id(b.getId())
                .teamId(b.getTeam().getId())
                .projectId(b.getProject() != null ? b.getProject().getId() : null)
                .name(b.getName())
                .boardType(b.getBoardType())
                .columnConfig(b.getColumnConfig())
                .swimlaneConfig(b.getSwimlaneConfig())
                .cardConfig(b.getCardConfig())
                .isDefault(b.getIsDefault())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

