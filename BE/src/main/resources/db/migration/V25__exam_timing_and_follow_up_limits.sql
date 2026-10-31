-- ============================================================================
-- V25: Server-side exam clock, per-answer time limit and follow-up budget
--   1. exams: answer_time_limit_seconds, max_follow_ups_per_question (lecturer settings)
--   2. exam_attempts: resumed_at (clock anchor) + both settings frozen per attempt
--   3. exam_turns: asked_at (answer timer start), timed_out (late answer flag)
-- Existing rows get the previous behaviour: 120 s per answer, 2 follow-ups.
-- ============================================================================

ALTER TABLE exams ADD COLUMN IF NOT EXISTS answer_time_limit_seconds INT NOT NULL DEFAULT 120;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS max_follow_ups_per_question INT NOT NULL DEFAULT 2;
ALTER TABLE exams ADD CONSTRAINT chk_exams_answer_time_limit
    CHECK (answer_time_limit_seconds BETWEEN 30 AND 600);
ALTER TABLE exams ADD CONSTRAINT chk_exams_max_follow_ups
    CHECK (max_follow_ups_per_question BETWEEN 0 AND 3);

ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS resumed_at TIMESTAMPTZ;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS answer_time_limit_seconds INT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS max_follow_ups_per_question INT;

-- Attempts already running keep counting from their start time.
UPDATE exam_attempts SET resumed_at = started_at
 WHERE status = 'IN_PROGRESS' AND resumed_at IS NULL;

ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS asked_at TIMESTAMPTZ;
ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS timed_out BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE exam_turns SET asked_at = created_at WHERE asked_at IS NULL;
