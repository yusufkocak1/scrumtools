package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Son gün olarak oluşturulan ve çözülen task sayısı.
 * date — yyyy-MM-dd
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatedVsResolvedDto {
    private String date;
    private int created;
    private int resolved;
}

