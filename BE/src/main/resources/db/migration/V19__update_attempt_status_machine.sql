-- ============================================================================
-- V19: Update Exam Attempt State Machine
-- Updates status constraints and adds timer/connection tracking columns:
--   1. status constraint: READY, IN_PROGRESS, RECONNECTING, FINALIZING, COMPLETED, INTERRUPTED
--   2. default status changed to 'READY'
--   3. started_at nullable (null until attempt is started)
--   4. new columns: paused_at, remaining_seconds (default 900), grace_deadline
-- ============================================================================

-- 1. Migrate any existing legacy status records
UPDATE exam_attempts
SET status = 'INTERRUPTED'
WHERE status IN ('TIMED_OUT', 'CANCELLED');

-- 2. Drop old check constraint and recreate with new statuses
ALTER TABLE exam_attempts DROP CONSTRAINT IF EXISTS exam_attempts_status_check;
ALTER TABLE exam_attempts ADD CONSTRAINT exam_attempts_status_check
    CHECK (status IN ('READY', 'IN_PROGRESS', 'RECONNECTING', 'FINALIZING', 'COMPLETED', 'INTERRUPTED', 'TIMED_OUT', 'CANCELLED'));

-- 3. Update default status and started_at nullability
ALTER TABLE exam_attempts ALTER COLUMN status SET DEFAULT 'READY';
ALTER TABLE exam_attempts ALTER COLUMN started_at DROP NOT NULL;

-- 4. Add timer and pause/reconnect tracking columns
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS remaining_seconds INT DEFAULT 900;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS grace_deadline TIMESTAMPTZ;

-- Backfill default remaining_seconds for any existing rows if null
UPDATE exam_attempts SET remaining_seconds = 900 WHERE remaining_seconds IS NULL;
