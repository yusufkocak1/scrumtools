package com.scrumtools.dto;

import com.scrumtools.entity.enums.FilterVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Zengin filtre oluşturma/güncelleme isteği (öğeler ayrı uçlardan yönetilir). */
@Data
public class RichFilterRequest {

    @NotBlank(message = "Zengin filtre adı zorunludur.")
    @Size(max = 120, message = "Ad en fazla 120 karakter olabilir.")
    private String name;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
    private String description;

    /** Temel sorgu — kayıtlı filtre referansı. Doluysa baseQuery yok sayılır. */
    private UUID baseFilterId;

    /** Temel sorgu — doğrudan STQL metni. */
    @Size(max = 2000, message = "Sorgu en fazla 2000 karakter olabilir.")
    private String baseQuery;

    /** PRIVATE | TEAM | PROJECT — belirtilmezse PRIVATE. */
    private FilterVisibility visibility;

    /** PROJECT görünürlüğünde zorunlu; diğerlerinde varsayılan kapsam. */
    private UUID projectId;
}
