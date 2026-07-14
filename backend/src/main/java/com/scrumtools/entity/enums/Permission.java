package com.scrumtools.entity.enums;

public enum Permission {
    // Proje Yönetimi
    PROJECT_CREATE,
    PROJECT_EDIT,
    PROJECT_DELETE,
    PROJECT_ARCHIVE,
    PROJECT_VIEW_SETTINGS,
    PROJECT_MANAGE_SETTINGS,

    // Takım Yönetimi
    TEAM_MANAGE_MEMBERS,
    TEAM_MANAGE_ROLES,
    TEAM_VIEW_MEMBERS,

    // İş Öğesi (Task/Issue) Yönetimi
    ISSUE_CREATE,
    ISSUE_EDIT_OWN,
    ISSUE_EDIT_ALL,
    ISSUE_DELETE,
    ISSUE_ASSIGN,
    ISSUE_CHANGE_STATUS,
    ISSUE_CHANGE_PRIORITY,
    ISSUE_MANAGE_LABELS,
    ISSUE_MANAGE_SUBTASKS,
    ISSUE_LINK,
    ISSUE_MOVE,

    // Sprint Yönetimi
    SPRINT_CREATE,
    SPRINT_START,
    SPRINT_COMPLETE,
    SPRINT_DELETE,
    SPRINT_EDIT,

    // Board Yönetimi
    BOARD_CREATE,
    BOARD_EDIT,
    BOARD_DELETE,

    // Yorum
    COMMENT_CREATE,
    COMMENT_EDIT_OWN,
    COMMENT_EDIT_ALL,
    COMMENT_DELETE_OWN,
    COMMENT_DELETE_ALL,

    // Dosya Yönetimi
    ATTACHMENT_UPLOAD,
    ATTACHMENT_DELETE_OWN,
    ATTACHMENT_DELETE_ALL,

    // Raporlama
    REPORT_VIEW,
    REPORT_EXPORT,

    // Workflow
    WORKFLOW_MANAGE,

    // Docs
    DOCS_MANAGE_SPACES,
    DOCS_WRITE,
    DOCS_READ,

    // SCM / Git Entegrasyonu
    SCM_CREATE_BRANCH,

    // Admin
    ADMIN_FULL_ACCESS
}
