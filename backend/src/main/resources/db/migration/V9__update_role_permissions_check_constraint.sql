-- role_permissions tablosundaki permission check constraint'ini güncelle.
-- Hibernate ddl-auto tarafından oluşturulan eski constraint, V7'de eklenen
-- DOCS_MANAGE_SPACES, DOCS_WRITE, DOCS_READ izinlerini içermiyor.

ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS role_permissions_permission_check;

ALTER TABLE role_permissions
    ADD CONSTRAINT role_permissions_permission_check CHECK (
        permission IN (
            'PROJECT_CREATE',
            'PROJECT_EDIT',
            'PROJECT_DELETE',
            'PROJECT_ARCHIVE',
            'PROJECT_VIEW_SETTINGS',
            'PROJECT_MANAGE_SETTINGS',
            'TEAM_MANAGE_MEMBERS',
            'TEAM_MANAGE_ROLES',
            'TEAM_VIEW_MEMBERS',
            'ISSUE_CREATE',
            'ISSUE_EDIT_OWN',
            'ISSUE_EDIT_ALL',
            'ISSUE_DELETE',
            'ISSUE_ASSIGN',
            'ISSUE_CHANGE_STATUS',
            'ISSUE_CHANGE_PRIORITY',
            'ISSUE_MANAGE_LABELS',
            'ISSUE_MANAGE_SUBTASKS',
            'ISSUE_LINK',
            'ISSUE_MOVE',
            'SPRINT_CREATE',
            'SPRINT_START',
            'SPRINT_COMPLETE',
            'SPRINT_DELETE',
            'SPRINT_EDIT',
            'BOARD_CREATE',
            'BOARD_EDIT',
            'BOARD_DELETE',
            'COMMENT_CREATE',
            'COMMENT_EDIT_OWN',
            'COMMENT_EDIT_ALL',
            'COMMENT_DELETE_OWN',
            'COMMENT_DELETE_ALL',
            'ATTACHMENT_UPLOAD',
            'ATTACHMENT_DELETE_OWN',
            'ATTACHMENT_DELETE_ALL',
            'REPORT_VIEW',
            'REPORT_EXPORT',
            'WORKFLOW_MANAGE',
            'DOCS_MANAGE_SPACES',
            'DOCS_WRITE',
            'DOCS_READ',
            'ADMIN_FULL_ACCESS'
        )
    );

