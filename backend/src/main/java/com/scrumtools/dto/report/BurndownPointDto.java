package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Burndown grafiği için tek veri noktası.
 * date  — sprint içindeki gün (ISO format: yyyy-MM-dd)
 * ideal — ideal kalan story point
 * actual — gerçek kalan story point (-1 → henüz o gün gelmemiş, gösterilmez)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BurndownPointDto {
    private String date;
    private double ideal;
    private double actual;
}

