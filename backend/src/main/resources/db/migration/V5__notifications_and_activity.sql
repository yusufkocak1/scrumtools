-- ─── Faz 5: Bildirim & Aktivite Sistemi ─────────────────────────────────────

-- notifications tablosu
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(255) NOT NULL,
    type            VARCHAR(60)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    entity_type     VARCHAR(60),
    entity_id       VARCHAR(255),
    data            JSONB,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_email);
CREATE INDEX idx_notifications_unread    ON notifications(recipient_email, is_read);
CREATE INDEX idx_notifications_created   ON notifications(created_at DESC);

-- activity_events tablosu
CREATE TABLE activity_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_email VARCHAR(255) NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    entity_type VARCHAR(60)  NOT NULL,
    entity_id   VARCHAR(255) NOT NULL,
    team_id     UUID         REFERENCES teams(id) ON DELETE SET NULL,
    details     JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_team    ON activity_events(team_id);
CREATE INDEX idx_activity_actor   ON activity_events(actor_email);
CREATE INDEX idx_activity_created ON activity_events(created_at DESC);

