package com.scrumtools.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class TaskRequest {
    private String title;
    private String description;
    private String status;
    private String issueType;
    private String priority;
    private String reporter;
    private String assignee;
    private String developer;
    private String analyst;
    private String tester;
    private Integer storyPoints;
    private List<String> labels;
    private String sprintId;
    private String parentTaskId;

    // Faz 3 alanları
    private LocalDate dueDate;
    private LocalDate startDate;
    private Double estimatedHours;
    private Double loggedHours;
    private String environment;
    private String resolution;
    private Integer position;

    /**
     * Genişleyebilir özel alanlar — ileride dashboard/arama/raporlama için kullanılacak.
     * Frontend herhangi bir key-value çifti gönderebilir.
     */
    private Map<String, Object> customFields;
}
