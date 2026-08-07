package com.scrumtools.dto;

import com.scrumtools.entity.CollabDocument;
import com.scrumtools.entity.enums.CollabDocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Liste ekranı satırı.
 *
 * <p>Tam {@code snapshotText} taşınmaz: bir kod dokümanı yüz kilobayt olabilir ve
 * liste ekranı onlarcasını birden çeker. Yerine kısa bir önizleme üretilir.
 */
public record CollabDocumentSummaryResponse(
        UUID id,
        CollabDocumentType type,
        String title,
        String language,
        UUID teamId,
        String teamName,
        String preview,
        boolean linkedToDocs,
        String updatedByName,
        LocalDateTime updatedAt
) {
    private static final int PREVIEW_LENGTH = 200;

    public static CollabDocumentSummaryResponse from(CollabDocument d) {
        return new CollabDocumentSummaryResponse(
                d.getId(),
                d.getType(),
                d.getTitle(),
                d.getLanguage(),
                d.getTeam() != null ? d.getTeam().getId() : null,
                d.getTeam() != null ? d.getTeam().getTeamName() : null,
                buildPreview(d),
                d.getDocPage() != null,
                d.getUpdatedBy() != null ? d.getUpdatedBy().getName() : null,
                d.getUpdatedAt()
        );
    }

    /**
     * TEXT dokümanlarda {@code snapshotText} HTML'dir; etiketleri düz metne
     * indirgemek listeyi hem okunur hem güvenli kılar (önizleme {@code v-html}
     * ile basılmaz).
     */
    private static String buildPreview(CollabDocument d) {
        String text = d.getSnapshotText();
        if (text == null || text.isBlank()) return "";
        if (d.getType() == CollabDocumentType.TEXT) {
            text = text.replaceAll("<[^>]+>", " ");
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }
}
