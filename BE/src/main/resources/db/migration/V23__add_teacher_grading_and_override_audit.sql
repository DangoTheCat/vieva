-- ============================================================================
-- V23: Add Teacher Grading and Override Audit Columns
-- Updates exam_turns and exam_scores to support:
--   1. Per-question teacher grading, feedback, actor audit, and timestamp
--   2. AI vs Teacher score breakdown in exam_scores
--   3. Teacher manual override audit with actor, timestamp, and reason
-- ============================================================================

-- ─── 1. EXAM_TURNS TEACHER GRADING COLUMNS ─────────────────────────────────
ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS teacher_score NUMERIC(5, 2);
ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS teacher_feedback TEXT;
ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS teacher_id UUID REFERENCES users (user_id) ON DELETE SET NULL;
ALTER TABLE exam_turns ADD COLUMN IF NOT EXISTS teacher_graded_at TIMESTAMPTZ;

-- ─── 2. EXAM_SCORES BREAKDOWN AND OVERRIDE AUDIT COLUMNS ───────────────────
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS ai_score NUMERIC(5, 2);
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS ai_feedback TEXT;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS teacher_score NUMERIC(5, 2);
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS teacher_feedback TEXT;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS teacher_id UUID REFERENCES users (user_id) ON DELETE SET NULL;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS override_total_score NUMERIC(5, 2);
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS override_by UUID REFERENCES users (user_id) ON DELETE SET NULL;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS override_at TIMESTAMPTZ;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS override_reason TEXT;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- ─── 3. BACKFILL AI SCORE/FEEDBACK FOR HISTORICAL RECORDS ──────────────────
UPDATE exam_scores SET ai_score = total_score WHERE ai_score IS NULL;
UPDATE exam_scores SET ai_feedback = detailed_feedback WHERE ai_feedback IS NULL;
