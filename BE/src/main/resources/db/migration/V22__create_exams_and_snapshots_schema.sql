-- ============================================================================
-- V22: Create Exams, Exam Questions, and Attempt Question Snapshots Schema
-- Creates schema for:
--   1. exams: lecturer exam definitions scoped to subjects
--   2. exam_questions: frozen selection of approved questions in an exam
--   3. exam_attempt_question_snapshots: frozen question, rubric, and answer snapshots per attempt
--   4. links exam_attempts to exams via exam_id
-- ============================================================================

-- ─── 1. EXAMS ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exams (
    id               UUID          PRIMARY KEY,
    subject_id       UUID          NOT NULL REFERENCES subjects (subject_id) ON DELETE RESTRICT,
    creator_id       UUID          NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT,
    title            VARCHAR(255)  NOT NULL,
    duration_seconds INT           NOT NULL DEFAULT 900,
    status           VARCHAR(50)   NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED')),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exams_subject_id ON exams (subject_id);
CREATE INDEX IF NOT EXISTS idx_exams_creator_id ON exams (creator_id);
CREATE INDEX IF NOT EXISTS idx_exams_status ON exams (status);

-- ─── 2. EXAM_QUESTIONS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exam_questions (
    id                  UUID        PRIMARY KEY,
    exam_id             UUID        NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    question_id         UUID        NOT NULL REFERENCES questions (question_id) ON DELETE RESTRICT,
    question_version_id UUID        NOT NULL REFERENCES question_versions (question_version_id) ON DELETE RESTRICT,
    order_index         INT         NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_exam_questions_exam_question UNIQUE (exam_id, question_id),
    CONSTRAINT uq_exam_questions_exam_order UNIQUE (exam_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_exam_questions_exam_id ON exam_questions (exam_id);

-- ─── 3. EXAM_ATTEMPT_QUESTION_SNAPSHOTS ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS exam_attempt_question_snapshots (
    id                  UUID        PRIMARY KEY,
    attempt_id          UUID        NOT NULL REFERENCES exam_attempts (id) ON DELETE CASCADE,
    question_id         UUID        NOT NULL,
    question_version_id UUID        NOT NULL,
    order_index         INT         NOT NULL,
    question_text       TEXT        NOT NULL,
    expected_answer     TEXT,
    rubric_snapshot     JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_attempt_snapshots_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT uq_attempt_snapshots_attempt_order UNIQUE (attempt_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_attempt_snapshots_attempt_id ON exam_attempt_question_snapshots (attempt_id);

-- ─── 4. ALTER EXAM_ATTEMPTS (ADD EXAM_ID) ───────────────────────────────────
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS exam_id UUID REFERENCES exams (id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_id ON exam_attempts (exam_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_attempts_exam_student
    ON exam_attempts (exam_id, student_id) WHERE exam_id IS NOT NULL;

-- ─── 5. ALTER COURSE_DOCUMENTS (ADD TOPIC_ID) ───────────────────────────────
ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS topic_id UUID REFERENCES topics (topic_id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_course_documents_topic_id ON course_documents (topic_id);
