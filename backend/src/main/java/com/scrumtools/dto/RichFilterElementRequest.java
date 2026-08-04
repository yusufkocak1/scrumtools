package com.scrumtools.dto;

import com.scrumtools.entity.enums.RichFilterElementKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/** Zengin filtre öğesi oluşturma/güncelleme isteği. */
@Data
public class RichFilterElementRequest {

    /** Güncellemede yok sayılır — öğenin türü değiştirilemez. */
    @NotNull(message = "Öğe türü zorunludur.")
    private RichFilterElementKind kind;

    @NotBlank(message = "Öğe adı zorunludur.")
    @Size(max = 120, message = "Ad en fazla 120 karakter olabilir.")
    private String name;

    /** STQL parçası — akıllı filtrelerde zorunlu. */
    @Size(max = 2000, message = "Sorgu en fazla 2000 karakter olabilir.")
    private String query;

    /** #RRGGBB. */
    @Size(max = 9)
    private String color;

    /** Türe özel alanlar; şeması RichFilterElement javadoc'unda. */
    private Map<String, Object> config;
}
