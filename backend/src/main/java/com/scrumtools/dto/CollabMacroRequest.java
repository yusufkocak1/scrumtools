package com.scrumtools.dto;

import com.scrumtools.entity.enums.MacroTriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Makro oluşturma / güncelleme (COLLAB_WORKSPACE_PLAN.md §6 — /macros). */
public record CollabMacroRequest(

        @NotBlank(message = "Makro adı zorunlu")
        @Size(max = 200)
        String name,

        @Size(max = 1000)
        String description,

        @NotBlank(message = "Makro kaynağı boş olamaz")
        @Size(max = 200_000, message = "Makro kaynağı çok uzun")
        String source,

        /** {@code null} ise proje kütüphanesi makrosu olur. */
        UUID documentId,

        MacroTriggerType triggerType,

        String scheduleCron,

        Boolean enabled
) {
}
