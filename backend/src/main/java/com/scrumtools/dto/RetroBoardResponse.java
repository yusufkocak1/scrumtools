package com.scrumtools.dto;

import com.scrumtools.entity.RetroBoard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RetroBoardResponse {
    private String id;
    private String teamId;
    private String retroBoardName;
    private List<String> columns;
    private LocalDateTime createdAt;

    public static RetroBoardResponse from(RetroBoard b) {
        return RetroBoardResponse.builder()
                .id(b.getId().toString())
                .teamId(b.getTeam().getId().toString())
                .retroBoardName(b.getRetroBoardName())
                .columns(b.getColumns())
                .createdAt(b.getCreatedAt())
                .build();
    }
}

