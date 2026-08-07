package com.scrumtools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Docs'a Kaydet" — ortak çalışma çıktısından yeni bir sayfa üretir (plan Y2). */
public record CollabPublishRequest(

        @NotNull(message = "Space seçilmeli")
        UUID spaceId,

        /** Boş bırakılırsa sayfa space kökünde açılır. */
        UUID parentPageId,

        @NotBlank(message = "Sayfa başlığı zorunlu")
        @Size(max = 500)
        String title
) {
}
