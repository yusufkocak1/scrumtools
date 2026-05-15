package com.scrumtools.dto;

import lombok.Data;

import java.util.List;

/**
 * Faz 4 — Dinamik task filtre isteği.
 * POST /api/teams/{teamId}/tasks/filter
 */
@Data
public class TaskFilterRequest {
    private List<TaskFilterCriteria> filters;

    /** Sıralama alanı: createdAt | dueDate | priority | status | title | position */
    private String sortBy;

    /** asc | desc */
    private String sortDir;

    private Integer page;
    private Integer size;
}

