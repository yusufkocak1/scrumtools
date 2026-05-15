package com.scrumtools.dto;

import com.scrumtools.entity.CodeShare;

import java.time.LocalDateTime;

public record CodeShareResponse(
        String id,
        String teamId,
        String tag,
        String data,
        LocalDateTime updatedAt
) {
    public static CodeShareResponse from(CodeShare codeShare) {
        return new CodeShareResponse(
                codeShare.getId().toString(),
                codeShare.getTeam().getId().toString(),
                codeShare.getTag(),
                codeShare.getData(),
                codeShare.getUpdatedAt()
        );
    }
}

