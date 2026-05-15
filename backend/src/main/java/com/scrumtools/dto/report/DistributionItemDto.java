package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Issue tipi veya öncelik dağılımı için tek kova (label + sayı).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistributionItemDto {
    private String label;
    private int count;
}

