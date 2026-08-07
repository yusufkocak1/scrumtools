package com.scrumtools.dto;

import com.scrumtools.entity.CollabMacro;
import com.scrumtools.entity.enums.MacroTriggerType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Makro detayı.
 *
 * <p>{@code approved} hesaplanmış bir alandır: {@code approvedAt} dolu olsa bile
 * kaynak onaydan sonra değiştiyse {@code false} döner. Arayüzün iki alanı
 * karşılaştırıp aynı sonucu yeniden türetmesi, kuralın iki yerde yaşaması demekti.
 */
public record CollabMacroResponse(
        UUID id,
        UUID projectId,
        UUID documentId,
        String name,
        String description,
        String source,
        MacroTriggerType triggerType,
        String scheduleCron,
        boolean enabled,
        boolean approved,
        List<String> apiScopes,
        String approvedByName,
        LocalDateTime approvedAt,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CollabMacroResponse from(CollabMacro macro, String currentHash) {
        return new CollabMacroResponse(
                macro.getId(),
                macro.getProject().getId(),
                macro.getDocument() != null ? macro.getDocument().getId() : null,
                macro.getName(),
                macro.getDescription(),
                macro.getSource(),
                macro.getTriggerType(),
                macro.getScheduleCron(),
                Boolean.TRUE.equals(macro.getEnabled()),
                macro.isApproved(currentHash),
                macro.getApiScopes() == null || macro.getApiScopes().isBlank()
                        ? List.of()
                        : List.of(macro.getApiScopes().split(",")),
                macro.getApprovedBy() != null ? macro.getApprovedBy().getName() : null,
                macro.getApprovedAt(),
                macro.getCreatedBy() != null ? macro.getCreatedBy().getName() : null,
                macro.getCreatedAt(),
                macro.getUpdatedAt());
    }
}
