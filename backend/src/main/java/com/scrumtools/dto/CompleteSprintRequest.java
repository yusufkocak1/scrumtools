package com.scrumtools.dto;

import lombok.Data;

/**
 * Sprint kapatılırken tamamlanmamış işlere ne olacağını taşır.
 *
 * incompleteAction:
 *  - BACKLOG   → görevlerin sprint bağı kaldırılır, backlog'a döner (varsayılan)
 *  - MOVE      → targetSprintId ile verilen sprinte taşınır
 *  - COMPLETE  → tamamlandı sayılır (status "Done")
 *  - KEEP      → kapanan sprintte kalır (sprint arşivi bozulmaz)
 */
@Data
public class CompleteSprintRequest {
    private String incompleteAction;
    private String targetSprintId;
}
