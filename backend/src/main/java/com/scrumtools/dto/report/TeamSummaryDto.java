package com.scrumtools.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Takım özet raporu — dashboard summary kartı için.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamSummaryDto {
    private int totalTasks;
    private int openTasks;
    private int inProgressTasks;
    private int doneTasks;
    private int overdueTasks;
    private int totalSprints;
    private int activeSprints;
    private List<DistributionItemDto> byPriority;
    private List<DistributionItemDto> byType;
    private List<DistributionItemDto> byStatus;
}

