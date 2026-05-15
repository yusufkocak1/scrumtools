package com.scrumtools.dto;

import lombok.Data;

import java.util.List;

/**
 * Tek filtre koşulu.
 * Desteklenen field değerleri: status, priority, issueType, assignee, reporter,
 *                              labels, dueDate, startDate, sprintId
 * Desteklenen operator değerleri: eq, neq, in, contains, gt, lt, is_null, is_not_null
 */
@Data
public class TaskFilterCriteria {
    private String field;
    private String operator; // eq | neq | in | contains | gt | lt | is_null | is_not_null
    private List<String> values;
}

