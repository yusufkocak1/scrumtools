package com.scrumtools.dto;

import com.scrumtools.entity.DocPage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DocPageResponse(
        UUID id,
        UUID spaceId,
        UUID parentPageId,
        String title,
        String slug,
        String content,
        int sortOrder,
        String createdByEmail,
        String createdByName,
        String updatedByEmail,
        String updatedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int versionNumber,
        List<DocPageResponse> children
) {
    public static DocPageResponse from(DocPage page, int versionNumber, List<DocPageResponse> children) {
        return new DocPageResponse(
                page.getId(),
                page.getSpace().getId(),
                page.getParentPage() != null ? page.getParentPage().getId() : null,
                page.getTitle(),
                page.getSlug(),
                page.getContent(),
                page.getSortOrder(),
                page.getCreatedBy().getEmail(),
                page.getCreatedBy().getName(),
                page.getUpdatedBy() != null ? page.getUpdatedBy().getEmail() : null,
                page.getUpdatedBy() != null ? page.getUpdatedBy().getName() : null,
                page.getCreatedAt(),
                page.getUpdatedAt(),
                versionNumber,
                children
        );
    }

    public static DocPageResponse fromFlat(DocPage page, int versionNumber) {
        return from(page, versionNumber, null);
    }
}

