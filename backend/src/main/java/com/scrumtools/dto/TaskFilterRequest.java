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

    /**
     * Aktif proje context'i — takım birden fazla projede çalışabildiği için sonuçlar
     * bu projeye daraltılır. Null ise takımın tüm projeleri ("Tüm projeler" görünümü).
     * Kullanıcı filtresi değil, kapsam kısıtıdır; filters ile birlikte AND'lenir.
     */
    private java.util.UUID projectId;

    /** Sıralama alanı: createdAt | dueDate | priority | status | title | position */
    private String sortBy;

    /** asc | desc */
    private String sortDir;

    private Integer page;
    private Integer size;
}

