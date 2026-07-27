package com.scrumtools.dto;

import com.scrumtools.entity.SavedFilter;
import com.scrumtools.entity.enums.FilterVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kayıtlı filtre yanıtı.
 *
 * @param owned     istekte bulunan kullanıcı filtrenin sahibi mi (düzenleme yetkisi)
 * @param favorite  kullanıcı bu filtreyi yıldızlamış mı
 */
public record SavedFilterResponse(
        UUID id,
        String name,
        String description,
        String query,
        FilterVisibility visibility,
        UUID projectId,
        String projectName,
        UUID teamId,
        String ownerEmail,
        String ownerName,
        boolean owned,
        boolean favorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SavedFilterResponse from(SavedFilter f, UUID currentUserId) {
        return new SavedFilterResponse(
                f.getId(),
                f.getName(),
                f.getDescription(),
                f.getQuery(),
                f.getVisibility(),
                f.getProject() != null ? f.getProject().getId() : null,
                f.getProject() != null ? f.getProject().getName() : null,
                f.getTeam().getId(),
                f.getOwner().getEmail(),
                f.getOwner().getName(),
                currentUserId != null && currentUserId.equals(f.getOwner().getId()),
                currentUserId != null && f.getFavoritedBy().contains(currentUserId),
                f.getCreatedAt(),
                f.getUpdatedAt());
    }
}
