-- ============================================================================
-- V5: Patch catalog refinements for Group 1 (UC1.1 - UC1.4)
-- 1. Add deleted_at to course_documents for soft-delete.
-- 2. Modify question_sources: allow chunk_id NULL, FK ON DELETE SET NULL, add document_name.
-- 3. Unique index for question_sources (question_version_id, chunk_id) where chunk_id IS NOT NULL.
-- ============================================================================

-- 1. Soft-delete support for course_documents
ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_documents_subject_deleted ON course_documents (subject_id, deleted_at);

-- 2. question_sources resilience: snapshot document_name & ON DELETE SET NULL for chunk_id
ALTER TABLE question_sources ADD COLUMN IF NOT EXISTS document_name VARCHAR(255);

ALTER TABLE question_sources ALTER COLUMN chunk_id DROP NOT NULL;

ALTER TABLE question_sources DROP CONSTRAINT IF EXISTS question_sources_chunk_id_fkey;
ALTER TABLE question_sources ADD CONSTRAINT question_sources_chunk_id_fkey
    FOREIGN KEY (chunk_id) REFERENCES document_chunks (chunk_id) ON DELETE SET NULL;

-- 3. Handle unique constraint on (question_version_id, chunk_id)
ALTER TABLE question_sources DROP CONSTRAINT IF EXISTS uq_question_version_chunk;
DROP INDEX IF EXISTS uq_question_version_chunk;
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_version_chunk
    ON question_sources (question_version_id, chunk_id)
    WHERE chunk_id IS NOT NULL;
