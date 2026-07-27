package com.scrumtools.dto;

import com.scrumtools.entity.enums.FilterVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Kayıtlı filtre oluşturma/güncelleme isteği. */
@Data
public class SavedFilterRequest {

    @NotBlank(message = "Filtre adı zorunludur.")
    @Size(max = 120, message = "Filtre adı en fazla 120 karakter olabilir.")
    private String name;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
    private String description;

    /** STQL metni. Boş sorgu da kaydedilebilir (tüm görevler görünümü). */
    @Size(max = 2000, message = "Sorgu en fazla 2000 karakter olabilir.")
    private String query;

    /** PRIVATE | TEAM | PROJECT — belirtilmezse PRIVATE. */
    private FilterVisibility visibility;

    /** PROJECT görünürlüğünde zorunlu; diğerlerinde varsayılan kapsam olarak kullanılır. */
    private UUID projectId;
}
