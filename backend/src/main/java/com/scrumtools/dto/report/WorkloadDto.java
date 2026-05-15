package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kullanıcı iş yükü: kaç açık task var ve toplam story point.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadDto {
    private String assigneeEmail;
    private String assigneeName;
    private int openTaskCount;
    private int totalStoryPoints;
}

