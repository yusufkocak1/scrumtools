package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint burndown raporu.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BurndownReportDto {
    private String sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalPoints;
    private List<BurndownPointDto> points;
}

