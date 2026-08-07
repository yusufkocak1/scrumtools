package com.scrumtools.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Doküman üstverisi güncelleme — {@code null} geçilen alana dokunulmaz.
 *
 * <p>İçerik buradan değiştirilemez: içerik CRDT akışına aittir ve yalnızca
 * anlık görüntü ucundan yazılır.
 */
public record CollabDocumentPatchRequest(

        @Size(max = 500, message = "Başlık en fazla 500 karakter olabilir")
        String title,

        @Size(max = 50)
        String language,

        UUID teamId,

        /** Takım etiketini kaldırmak için: {@code teamId} null iken bunu true yapın. */
        Boolean clearTeam
) {
}
