package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tek bir sprint'in velocity bilgisi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SprintVelocityDto {
    private String sprintId;
    private String sprintName;
    private int committed;   // sprint başındaki toplam story point
    private int completed;   // Done statüsündeki task'ların story point toplamı
}

