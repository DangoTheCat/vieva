-- ============================================================================
-- V21: Rubric Templates and Document Topic/Metadata Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS rubric_templates (
    id                  UUID          PRIMARY KEY,
    subject_id          UUID          NOT NULL REFERENCES subjects (subject_id) ON DELETE CASCADE,
    name                VARCHAR(255)  NOT NULL,
    description         TEXT,
    criteria_json       JSONB         NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rubric_templates_subject_id ON rubric_templates (subject_id);

ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS topic_id UUID REFERENCES topics (topic_id) ON DELETE SET NULL;
ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS metadata_json JSONB;
