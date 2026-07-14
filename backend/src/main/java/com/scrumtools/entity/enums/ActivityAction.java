package com.scrumtools.entity.enums;

public enum ActivityAction {
    // Task aksiyonları
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    TASK_STATUS_CHANGED,
    TASK_ASSIGNED,
    TASK_COMMENTED,
    TASK_ATTACHMENT_ADDED,

    // Sprint aksiyonları
    SPRINT_CREATED,
    SPRINT_STARTED,
    SPRINT_COMPLETED,

    // Team aksiyonları
    MEMBER_ADDED,
    MEMBER_REMOVED,
    MEMBER_ROLE_CHANGED,

    // Board aksiyonları
    BOARD_CREATED,
    BOARD_UPDATED,

    // SCM / Git aksiyonları
    SCM_BRANCH_CREATED,
    SCM_COMMIT_LINKED
}

