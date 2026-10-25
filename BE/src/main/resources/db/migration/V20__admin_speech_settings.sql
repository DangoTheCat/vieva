-- ============================================================================
-- V20: Speech Settings Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS speech_settings (
    id                  UUID          PRIMARY KEY,
    locale              VARCHAR(50)   NOT NULL,
    voice               VARCHAR(100)  NOT NULL,
    speed               NUMERIC(3, 2) NOT NULL DEFAULT 1.0,
    pitch               NUMERIC(3, 2) NOT NULL DEFAULT 1.0,
    updated_by          UUID          REFERENCES users (user_id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

INSERT INTO speech_settings (id, locale, voice, speed, pitch, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'vi-VN', 'maichi', 1.0, 1.0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
