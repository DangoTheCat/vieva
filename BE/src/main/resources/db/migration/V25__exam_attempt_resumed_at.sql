-- Start of the current IN_PROGRESS segment: remaining time = remaining_seconds - (now - resumed_at)
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS resumed_at TIMESTAMPTZ;

UPDATE exam_attempts SET resumed_at = started_at
WHERE status = 'IN_PROGRESS' AND resumed_at IS NULL;
