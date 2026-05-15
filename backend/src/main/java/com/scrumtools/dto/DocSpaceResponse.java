package com.scrumtools.dto;

import com.scrumtools.entity.DocSpace;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocSpaceResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        String createdByEmail,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int pageCount
) {
    public static DocSpaceResponse from(DocSpace space, int pageCount) {
        return new DocSpaceResponse(
                space.getId(),
                space.getProject().getId(),
                space.getName(),
                space.getDescription(),
                space.getCreatedBy().getEmail(),
                space.getCreatedBy().getName(),
                space.getCreatedAt(),
                space.getUpdatedAt(),
                pageCount
        );
    }
}

