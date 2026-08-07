package com.scrumtools.dto;

import com.scrumtools.entity.enums.CollabDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Yeni ortak çalışma dokümanı (COLLAB_WORKSPACE_PLAN.md §6). */
public record CollabDocumentRequest(

        @NotNull(message = "Doküman tipi zorunlu")
        CollabDocumentType type,

        @NotBlank(message = "Başlık zorunlu")
        @Size(max = 500, message = "Başlık en fazla 500 karakter olabilir")
        String title,

        /** Yalnızca CODE için: Monaco dil kimliği. */
        @Size(max = 50)
        String language,

        /** İsteğe bağlı takım etiketi — yetkiye etkisi yoktur (plan D4). */
        UUID teamId
) {
}
