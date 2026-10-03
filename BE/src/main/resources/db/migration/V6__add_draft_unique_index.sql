-- One DRAFT version per question.
-- Guards the concurrent create-draft race (check-then-act on hasDraftVersion):
-- the loser of the race hits this index and surfaces DRAFT_ALREADY_EXISTS.
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_draft_per_question
    ON question_versions (question_id)
    WHERE approval_status = 'DRAFT';
