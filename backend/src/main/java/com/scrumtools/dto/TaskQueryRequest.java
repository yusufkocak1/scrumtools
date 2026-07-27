package com.scrumtools.dto;

import lombok.Data;

import java.util.UUID;

/**
 * STQL sorgu isteği.
 * POST /api/teams/{teamId}/tasks/query
 */
@Data
public class TaskQueryRequest {

    /** STQL metni. Boş bırakılırsa kapsamdaki tüm görevler döner. */
    private String query;

    /**
     * Aktif proje kapsamı — kullanıcı filtresi değil, kapsam kısıtıdır ve
     * sorgudan bağımsız olarak AND'lenir. Null ise takımın tüm projeleri.
     */
    private UUID projectId;

    private Integer page;
    private Integer size;
}
