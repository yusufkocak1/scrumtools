package com.scrumtools.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Gruplama isteği — grafik widget'larının veri kaynağı.
 * POST /api/teams/{teamId}/tasks/query/aggregate
 */
@Data
public class TaskAggregateRequest {

    /** STQL metni. Boş bırakılırsa kapsamdaki tüm görevler gruplanır. */
    private String query;

    /** Aktif proje kapsamı — sorgudan bağımsız olarak AND'lenir. */
    private UUID projectId;

    /** Gruplanacak alan: status, priority, assignee, labels, sprint… */
    private String groupBy;

    /** "count" (varsayılan) ya da toplanacak sayısal alan: storyPoints, loggedHours… */
    private String metric;

    /** Dönecek kova sayısı; aşan gruplar "Diğer" kovasında toplanır. */
    private Integer limit;
}
