-- ============================================================================
-- V18: Oral Exam Schema
-- Creates tables for the oral examination workflow:
--   1. exam_attempts: records a student's oral exam session for a subject
--   2. exam_turns: records turns (main question & follow-up) in an exam attempt
--   3. exam_scores: records the final overall grading and detailed feedback
-- ============================================================================

-- ─── 1. EXAM_ATTEMPTS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exam_attempts (
    id                     UUID          PRIMARY KEY,
    student_id             UUID          NOT NULL REFERENCES users (user_id),
    subject_id             UUID          NOT NULL REFERENCES subjects (subject_id),
    status                 VARCHAR(50)   NOT NULL DEFAULT 'IN_PROGRESS'
                           CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'TIMED_OUT', 'CANCELLED')),
    current_question_index INT           NOT NULL DEFAULT 0,
    total_questions        INT           NOT NULL DEFAULT 0,
    total_score            NUMERIC(5, 2),
    grade_level            VARCHAR(50)   CHECK (grade_level IS NULL OR grade_level IN ('EXCELLENT', 'GOOD', 'AVERAGE', 'POOR')),
    started_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Indexes for exam_attempts
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_id ON exam_attempts (student_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_subject_id ON exam_attempts (subject_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status ON exam_attempts (status);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_status ON exam_attempts (student_id, status);

-- ─── 2. EXAM_TURNS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exam_turns (
    id                  UUID          PRIMARY KEY,
    attempt_id          UUID          NOT NULL REFERENCES exam_attempts (id) ON DELETE CASCADE,
    question_id         UUID          REFERENCES questions (question_id),
    question_version_id UUID          REFERENCES question_versions (question_version_id),
    turn_type           VARCHAR(50)   NOT NULL DEFAULT 'MAIN_QUESTION'
                        CHECK (turn_type IN ('MAIN_QUESTION', 'FOLLOW_UP')),
    parent_turn_id      UUID          REFERENCES exam_turns (id) ON DELETE SET NULL,
    turn_order          INT           NOT NULL DEFAULT 1,
    question_text       TEXT          NOT NULL,
    tts_audio_url       TEXT,
    student_audio_url   TEXT,
    transcript_text     TEXT,
    status              VARCHAR(50)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ANSWERED', 'EVALUATED')),
    score               NUMERIC(5, 2),
    feedback            TEXT,
    rubric_evaluations  JSONB,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Indexes for exam_turns
CREATE INDEX IF NOT EXISTS idx_exam_turns_attempt_id ON exam_turns (attempt_id);
CREATE INDEX IF NOT EXISTS idx_exam_turns_status ON exam_turns (status);
CREATE INDEX IF NOT EXISTS idx_exam_turns_attempt_order ON exam_turns (attempt_id, turn_order);
CREATE INDEX IF NOT EXISTS idx_exam_turns_parent_id ON exam_turns (parent_turn_id);

-- ─── 3. EXAM_SCORES ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exam_scores (
    id                UUID          PRIMARY KEY,
    attempt_id        UUID          NOT NULL UNIQUE REFERENCES exam_attempts (id) ON DELETE CASCADE,
    total_score       NUMERIC(5, 2) NOT NULL,
    grade             VARCHAR(50)   NOT NULL,
    detailed_feedback TEXT,
    scoring_mode      VARCHAR(50)   NOT NULL DEFAULT 'GEMINI_FLASH_LITE'
                      CHECK (scoring_mode IN ('GEMINI_FLASH_LITE', 'RULE_BASED_FALLBACK')),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Indexes for exam_scores
CREATE INDEX IF NOT EXISTS idx_exam_scores_attempt_id ON exam_scores (attempt_id);
