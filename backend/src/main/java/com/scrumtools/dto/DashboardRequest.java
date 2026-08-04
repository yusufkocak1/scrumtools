package com.scrumtools.dto;

import com.scrumtools.entity.enums.DashboardVisibility;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Pano oluşturma/güncelleme isteği.
 *
 * {@code layout} isteğe bağlıdır: yeniden adlandırma ya da görünürlük değişikliği
 * düzeni taşımadan gelir ve o durumda mevcut düzen korunur. Boş bir pano ile
 * "düzeni gönderilmeyen pano" ayrımı bu yüzden null üzerinden yapılır.
 */
@Data
public class DashboardRequest {

    private String name;

    private DashboardVisibility visibility;

    private List<Map<String, Object>> layout;

    /** Sekme sırası; verilmezse mevcut sıra korunur. */
    private Integer position;
}
