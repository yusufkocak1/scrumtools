package com.scrumtools.entity.enums;

/**
 * Workflow status'ların raporlama/board gruplama kategorisi.
 * Hangi iş akışı kullanılırsa kullanılsın burndown/velocity hesaplamaları için standart gruplama sağlar.
 */
public enum StatusCategory {
    /** Henüz başlanmamış işler (To Do, Open, Backlog, New vb.) */
    TO_DO,
    /** Devam eden işler (In Progress, In Review, Testing vb.) */
    IN_PROGRESS,
    /** Tamamlanmış işler (Done, Closed, Fixed, Cancelled vb.) */
    DONE
}

