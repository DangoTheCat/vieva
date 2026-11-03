-- ============================================================================
-- V28: Lecturer finalizes ("chốt điểm") a grade before the student can see it
--   exam_scores.published_at / published_by: NULL = not finalized yet, so the
--   student sees no score, grade or AI feedback.
-- Existing scores the lecturer already graded or overrode count as finalized;
-- AI-only scores stay hidden until a lecturer finalizes them.
-- ============================================================================

ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;
ALTER TABLE exam_scores ADD COLUMN IF NOT EXISTS published_by UUID REFERENCES users (user_id) ON DELETE SET NULL;

UPDATE exam_scores
   SET published_at = COALESCE(override_at, updated_at, created_at),
       published_by = COALESCE(override_by, teacher_id)
 WHERE published_at IS NULL
   AND (teacher_score IS NOT NULL OR override_total_score IS NOT NULL);
