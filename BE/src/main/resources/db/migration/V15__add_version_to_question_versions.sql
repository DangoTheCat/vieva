-- V15: Add optimistic locking version column to question_versions if missing
ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
