-- Apply manually to existing databases after a backup. No Flyway runtime required.
BEGIN;
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS head_lecturer_id UUID REFERENCES users(user_id);
ALTER TABLE course_documents DROP CONSTRAINT IF EXISTS ck_documents_indexing_status;
ALTER TABLE course_documents DROP CONSTRAINT IF EXISTS ck_course_documents_status;
ALTER TABLE course_documents ADD CONSTRAINT ck_documents_indexing_status
    CHECK (indexing_status IN ('UPLOADED','INDEXING','READY','ARCHIVED','FAILED'));
ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS ck_qversions_approval_status;
ALTER TABLE question_versions ADD CONSTRAINT ck_qversions_approval_status
    CHECK (approval_status IN ('DRAFT','UNDER_REVIEW','APPROVED','SUPERSEDED','ARCHIVED','REJECTED'));
DROP INDEX IF EXISTS uq_question_draft_per_question;
CREATE UNIQUE INDEX uq_question_draft_per_question ON question_versions(question_id)
    WHERE approval_status IN ('DRAFT','UNDER_REVIEW');
CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(user_id),
    question_id UUID NOT NULL REFERENCES questions(question_id),
    version_id UUID NOT NULL REFERENCES question_versions(question_version_id),
    type VARCHAR(40) NOT NULL CHECK(type IN ('PENDING_REVIEW','QUESTION_APPROVED')),
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id,created_at DESC);
COMMIT;
