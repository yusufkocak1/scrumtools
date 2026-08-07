package com.scrumtools.dto;

import com.scrumtools.entity.CollabDocument;
import com.scrumtools.entity.enums.CollabDocumentType;
import com.scrumtools.entity.enums.CollabLinkMode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Doküman detayı. {@code snapshotText} salt okuma için yeterlidir — düzenlemeye
 * girmeden içeriği göstermek isteyen ekranlar CRDT durumunu indirmez.
 */
public record CollabDocumentResponse(
        UUID id,
        UUID projectId,
        UUID teamId,
        String teamName,
        CollabDocumentType type,
        String title,
        String language,
        UUID docPageId,
        /** Docs'a bağlıysa sayfanın alanı — arayüzün sayfa linkini kurabilmesi için. */
        UUID docSpaceId,
        CollabLinkMode linkMode,
        String snapshotText,
        Long lastSeq,
        boolean canWrite,
        String createdByName,
        String updatedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CollabDocumentResponse from(CollabDocument d, boolean canWrite) {
        return new CollabDocumentResponse(
                d.getId(),
                d.getProject().getId(),
                d.getTeam() != null ? d.getTeam().getId() : null,
                d.getTeam() != null ? d.getTeam().getTeamName() : null,
                d.getType(),
                d.getTitle(),
                d.getLanguage(),
                d.getDocPage() != null ? d.getDocPage().getId() : null,
                d.getDocPage() != null ? d.getDocPage().getSpace().getId() : null,
                d.getLinkMode(),
                d.getSnapshotText(),
                d.getLastSeq(),
                canWrite,
                d.getCreatedBy() != null ? d.getCreatedBy().getName() : null,
                d.getUpdatedBy() != null ? d.getUpdatedBy().getName() : null,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
