package com.scrumtools.dto;

import com.scrumtools.entity.DocPageVersion;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocPageVersionResponse(
        UUID id,
        UUID pageId,
        int versionNumber,
        String title,
        String content,
        String changeSummary,
        String createdByEmail,
        String createdByName,
        LocalDateTime createdAt
) {
    public static DocPageVersionResponse from(DocPageVersion v) {
        return new DocPageVersionResponse(
                v.getId(),
                v.getPage().getId(),
                v.getVersionNumber(),
                v.getTitle(),
                v.getContent(),
                v.getChangeSummary(),
                v.getCreatedBy().getEmail(),
                v.getCreatedBy().getName(),
                v.getCreatedAt()
        );
    }
}

