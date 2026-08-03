package com.scrumtools.dto;

import com.scrumtools.entity.RichFilter;
import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.FilterVisibility;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Zengin filtre yanıtı.
 *
 * @param owned    istekte bulunan kullanıcı sahibi mi (düzenleme yetkisi)
 * @param elements sıralı öğe listesi — akıllı filtrelerde sıra sonucu etkiler
 */
public record RichFilterResponse(
        UUID id,
        String name,
        String description,
        UUID baseFilterId,
        String baseFilterName,
        String baseQuery,
        String effectiveQuery,
        FilterVisibility visibility,
        UUID projectId,
        String projectName,
        UUID teamId,
        String ownerEmail,
        String ownerName,
        boolean owned,
        List<RichFilterElementResponse> elements,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RichFilterResponse from(RichFilter r, UUID currentUserId) {
        List<RichFilterElementResponse> elements = r.getElements().stream()
                .sorted(Comparator.comparing(RichFilterElement::getKind)
                        .thenComparingInt(e -> e.getPosition() == null ? 0 : e.getPosition()))
                .map(RichFilterElementResponse::from)
                .toList();

        return new RichFilterResponse(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getBaseFilter() != null ? r.getBaseFilter().getId() : null,
                r.getBaseFilter() != null ? r.getBaseFilter().getName() : null,
                r.getBaseQuery(),
                r.effectiveBaseQuery(),
                r.getVisibility(),
                r.effectiveProject() != null ? r.effectiveProject().getId() : null,
                r.effectiveProject() != null ? r.effectiveProject().getName() : null,
                r.getTeam().getId(),
                r.getOwner().getEmail(),
                r.getOwner().getName(),
                currentUserId != null && currentUserId.equals(r.getOwner().getId()),
                elements,
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
