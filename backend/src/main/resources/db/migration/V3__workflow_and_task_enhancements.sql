-- V3: TeamMember User FK + Workflow Engine + Task Enhancements

-- ─── 1. TeamMember: user_id FK ekle ─────────────────────────────────────────
ALTER TABLE team_members
    ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE SET NULL;

-- Mevcut kayıtları e-posta üzerinden user_id ile eşleştir (best-effort migration)
UPDATE team_members tm
SET user_id = u.id
FROM users u
WHERE tm.email = u.email AND tm.user_id IS NULL;

-- ─── 2. Task: Yeni Faz 3 alanları ────────────────────────────────────────────
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS parent_task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reporter       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS due_date       DATE,
    ADD COLUMN IF NOT EXISTS start_date     DATE,
    ADD COLUMN IF NOT EXISTS estimated_hours DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS logged_hours    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS environment    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS resolution     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS resolved_at    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS position       INTEGER NOT NULL DEFAULT 0;

-- task_watchers bağlantı tablosu
CREATE TABLE IF NOT EXISTS task_watchers (
    task_id       UUID         NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    watcher_email VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, watcher_email)
);

CREATE INDEX IF NOT EXISTS idx_task_parent ON tasks(parent_task_id);

-- ─── 3. Task History (Audit Log) ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS task_history (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID         NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    field       VARCHAR(100) NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    changed_by  VARCHAR(255) NOT NULL,
    changed_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_history_task       ON task_history(task_id);
CREATE INDEX IF NOT EXISTS idx_history_changed_at ON task_history(changed_at);

-- ─── 4. Task Links ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS task_links (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_task_id UUID         NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    target_task_id UUID         NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    link_type      VARCHAR(50)  NOT NULL,
    created_by     VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_task_link UNIQUE (source_task_id, target_task_id, link_type)
);

CREATE INDEX IF NOT EXISTS idx_tlink_source ON task_links(source_task_id);
CREATE INDEX IF NOT EXISTS idx_tlink_target ON task_links(target_task_id);

-- ─── 5. Workflow Engine ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS workflows (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID         REFERENCES projects(id) ON DELETE CASCADE,
    team_id     UUID         REFERENCES teams(id)    ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_issue_types (
    workflow_id UUID        NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    issue_type  VARCHAR(50) NOT NULL,
    PRIMARY KEY (workflow_id, issue_type)
);

CREATE TABLE IF NOT EXISTS workflow_statuses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID         NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(20)  NOT NULL DEFAULT 'TO_DO',
    color       VARCHAR(20)  NOT NULL DEFAULT '#6B7280',
    icon        VARCHAR(50),
    position    INTEGER      NOT NULL DEFAULT 0,
    is_initial  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_final    BOOLEAN      NOT NULL DEFAULT FALSE,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_wstatus_workflow ON workflow_statuses(workflow_id);

CREATE TABLE IF NOT EXISTS workflow_transitions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id    UUID         NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,
    from_status_id UUID         REFERENCES workflow_statuses(id) ON DELETE CASCADE,
    to_status_id   UUID         NOT NULL REFERENCES workflow_statuses(id) ON DELETE CASCADE,
    conditions     JSONB,
    actions        JSONB,
    position       INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_wtrans_workflow ON workflow_transitions(workflow_id);

CREATE TABLE IF NOT EXISTS workflow_transition_roles (
    transition_id UUID NOT NULL REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    role_id       UUID NOT NULL,
    PRIMARY KEY (transition_id, role_id)
);

-- ─── 6. Custom Field Definitions ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id       UUID         REFERENCES teams(id)    ON DELETE CASCADE,
    project_id    UUID         REFERENCES projects(id) ON DELETE CASCADE,
    name          VARCHAR(200) NOT NULL,
    field_key     VARCHAR(100) NOT NULL,
    field_type    VARCHAR(50)  NOT NULL,
    options       JSONB,
    is_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_visible    BOOLEAN      NOT NULL DEFAULT TRUE,
    default_value TEXT,
    position      INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS custom_field_issue_types (
    field_id   UUID        NOT NULL REFERENCES custom_field_definitions(id) ON DELETE CASCADE,
    issue_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (field_id, issue_type)
);

