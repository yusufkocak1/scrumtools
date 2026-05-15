CREATE TABLE user_dashboards
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    layout     JSONB       NOT NULL DEFAULT '[]',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_dashboard UNIQUE (user_id)
);

CREATE INDEX idx_user_dashboards_user ON user_dashboards (user_id);

