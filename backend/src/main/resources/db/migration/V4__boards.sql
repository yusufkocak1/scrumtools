-- Faz 4: Board & Görünüm Katmanı
-- Boards tablosu

CREATE TABLE IF NOT EXISTS boards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    project_id      UUID REFERENCES projects(id) ON DELETE SET NULL,
    name            VARCHAR(255) NOT NULL DEFAULT 'Board',
    board_type      VARCHAR(50)  NOT NULL DEFAULT 'KANBAN',
    column_config   JSONB        NOT NULL DEFAULT '{}',
    swimlane_config JSONB        NOT NULL DEFAULT '{}',
    card_config     JSONB        NOT NULL DEFAULT '{}',
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_board_team ON boards(team_id);

