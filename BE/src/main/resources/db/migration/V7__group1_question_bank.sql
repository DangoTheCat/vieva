-- ============================================================================
-- V7: Group 1 — question bank, rubric and RAG (UC1.1 - UC1.7, WF01)
--   * Course documents: storage key, bounded indexing retries, status CHECK
--   * Chunks: HNSW cosine index, no cascade delete from documents
--   * Questions: subject ownership, current approved version, optimistic lock
--   * Question versions: 6-level Bloom CHECK, bloom confirmation, copy-on-write parent,
--     link to the generation request (regeneration context), optimistic lock timestamps
--   * Question sources: document reference + ordering
--   * Rubric criteria: performance levels (JSONB)
--   * question_generation_requests: persisted RAG context for regeneration and retries
--   * Evidence-bearing FKs switched from ON DELETE CASCADE to RESTRICT (BR-04)
-- ============================================================================

-- ─── COURSE_DOCUMENTS ──────────────────────────────────────────────────────
ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);
ALTER TABLE course_documents ADD COLUMN IF NOT EXISTS index_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE course_documents DROP CONSTRAINT IF EXISTS ck_course_documents_status;
ALTER TABLE course_documents ADD CONSTRAINT ck_course_documents_status
    CHECK (indexing_status IN ('UPLOADED', 'INDEXING', 'READY', 'FAILED'));

-- ─── DOCUMENT_CHUNKS ───────────────────────────────────────────────────────
ALTER TABLE document_chunks DROP CONSTRAINT IF EXISTS document_chunks_document_id_fkey;
ALTER TABLE document_chunks ADD CONSTRAINT document_chunks_document_id_fkey
    FOREIGN KEY (document_id) REFERENCES course_documents (document_id);

CREATE INDEX IF NOT EXISTS idx_chunks_embedding_hnsw
    ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- ─── QUESTIONS ─────────────────────────────────────────────────────────────
ALTER TABLE questions ADD COLUMN IF NOT EXISTS subject_id UUID;
UPDATE questions q
SET subject_id = t.subject_id
FROM topics t
WHERE q.topic_id = t.topic_id
  AND q.subject_id IS NULL;
ALTER TABLE questions ALTER COLUMN subject_id SET NOT NULL;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS fk_questions_subject;
ALTER TABLE questions ADD CONSTRAINT fk_questions_subject
    FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);

-- Topic is optional: a question belongs to a subject, the topic only refines it.
ALTER TABLE questions ALTER COLUMN topic_id DROP NOT NULL;

ALTER TABLE questions ADD COLUMN IF NOT EXISTS current_approved_version_id UUID;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE questions DROP CONSTRAINT IF EXISTS ck_questions_status;
ALTER TABLE questions ADD CONSTRAINT ck_questions_status
    CHECK (status IN ('ACTIVE', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS idx_questions_subject_status ON questions (subject_id, status);

-- ─── QUESTION_VERSIONS ─────────────────────────────────────────────────────
-- Map the legacy 4-level Bloom scale onto the revised 6-level taxonomy before the CHECK.
UPDATE question_versions
SET bloom_level = CASE bloom_level
        WHEN 'NHAN_BIET'     THEN 'REMEMBER'
        WHEN 'THONG_HIEU'    THEN 'UNDERSTAND'
        WHEN 'VAN_DUNG'      THEN 'APPLY'
        WHEN 'VAN_DUNG_CAO'  THEN 'ANALYZE'
        WHEN 'REMEMBERING'   THEN 'REMEMBER'
        WHEN 'UNDERSTANDING' THEN 'UNDERSTAND'
        WHEN 'APPLYING'      THEN 'APPLY'
        WHEN 'ANALYZING'     THEN 'ANALYZE'
        WHEN 'EVALUATING'    THEN 'EVALUATE'
        WHEN 'CREATING'      THEN 'CREATE'
        ELSE bloom_level
    END
WHERE bloom_level NOT IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE');

ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS bloom_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
-- Human-authored or already reviewed versions count as confirmed.
UPDATE question_versions
SET bloom_confirmed = TRUE
WHERE generation_mode <> 'AI_RAG'
   OR approval_status IN ('APPROVED', 'SUPERSEDED');

ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS parent_version_id UUID;
ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS generation_request_id UUID;
ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS regeneration_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE question_versions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS fk_qversions_parent;
ALTER TABLE question_versions ADD CONSTRAINT fk_qversions_parent
    FOREIGN KEY (parent_version_id) REFERENCES question_versions (question_version_id);

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS ck_qversions_bloom_level;
ALTER TABLE question_versions ADD CONSTRAINT ck_qversions_bloom_level
    CHECK (bloom_level IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'));

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS ck_qversions_approval_status;
ALTER TABLE question_versions ADD CONSTRAINT ck_qversions_approval_status
    CHECK (approval_status IN ('DRAFT', 'APPROVED', 'REJECTED', 'SUPERSEDED'));

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS ck_qversions_generation_mode;
ALTER TABLE question_versions ADD CONSTRAINT ck_qversions_generation_mode
    CHECK (generation_mode IN ('AI_RAG', 'MANUAL', 'IMPORT'));

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS ck_qversions_numbers;
ALTER TABLE question_versions ADD CONSTRAINT ck_qversions_numbers
    CHECK (version_number >= 1 AND regeneration_count >= 0);

-- Versions are evidence: never cascade-delete them with the question.
ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS question_versions_question_id_fkey;
ALTER TABLE question_versions ADD CONSTRAINT question_versions_question_id_fkey
    FOREIGN KEY (question_id) REFERENCES questions (question_id);

CREATE INDEX IF NOT EXISTS idx_qversions_status ON question_versions (approval_status);
CREATE INDEX IF NOT EXISTS idx_qversions_generation_request ON question_versions (generation_request_id);

-- Backfill the published pointer from the latest APPROVED version, then add the FK.
UPDATE questions q
SET current_approved_version_id = v.question_version_id
FROM (SELECT DISTINCT ON (question_id) question_id, question_version_id
      FROM question_versions
      WHERE approval_status = 'APPROVED'
      ORDER BY question_id, version_number DESC) v
WHERE v.question_id = q.question_id
  AND q.current_approved_version_id IS NULL;

ALTER TABLE questions DROP CONSTRAINT IF EXISTS fk_questions_current_version;
ALTER TABLE questions ADD CONSTRAINT fk_questions_current_version
    FOREIGN KEY (current_approved_version_id) REFERENCES question_versions (question_version_id);

-- ─── QUESTION_SOURCES ──────────────────────────────────────────────────────
ALTER TABLE question_sources ADD COLUMN IF NOT EXISTS document_id UUID;
ALTER TABLE question_sources ADD COLUMN IF NOT EXISTS source_order INTEGER NOT NULL DEFAULT 1;

UPDATE question_sources s
SET document_id = c.document_id
FROM document_chunks c
WHERE s.chunk_id = c.chunk_id
  AND s.document_id IS NULL;

ALTER TABLE question_sources DROP CONSTRAINT IF EXISTS fk_question_sources_document;
ALTER TABLE question_sources ADD CONSTRAINT fk_question_sources_document
    FOREIGN KEY (document_id) REFERENCES course_documents (document_id);

ALTER TABLE question_sources DROP CONSTRAINT IF EXISTS question_sources_question_version_id_fkey;
ALTER TABLE question_sources ADD CONSTRAINT question_sources_question_version_id_fkey
    FOREIGN KEY (question_version_id) REFERENCES question_versions (question_version_id);

-- Chunks referenced as evidence must not disappear (V5 used ON DELETE SET NULL).
ALTER TABLE question_sources DROP CONSTRAINT IF EXISTS question_sources_chunk_id_fkey;
ALTER TABLE question_sources ADD CONSTRAINT question_sources_chunk_id_fkey
    FOREIGN KEY (chunk_id) REFERENCES document_chunks (chunk_id);

CREATE INDEX IF NOT EXISTS idx_question_sources_document ON question_sources (document_id);

-- ─── RUBRICS / RUBRIC_CRITERIA ─────────────────────────────────────────────
ALTER TABLE rubrics DROP CONSTRAINT IF EXISTS fk_rubrics_question_version;
ALTER TABLE rubrics ADD CONSTRAINT fk_rubrics_question_version
    FOREIGN KEY (question_version_id) REFERENCES question_versions (question_version_id);

ALTER TABLE rubrics DROP CONSTRAINT IF EXISTS ck_rubrics_total_points;
ALTER TABLE rubrics ADD CONSTRAINT ck_rubrics_total_points CHECK (total_points >= 0);

ALTER TABLE rubric_criteria ADD COLUMN IF NOT EXISTS performance_levels JSONB;

ALTER TABLE rubric_criteria DROP CONSTRAINT IF EXISTS ck_rubric_criteria_max_points;
ALTER TABLE rubric_criteria ADD CONSTRAINT ck_rubric_criteria_max_points CHECK (max_points > 0);

-- ─── QUESTION_GENERATION_REQUESTS ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_generation_requests (
    generation_request_id UUID        PRIMARY KEY,
    subject_id            UUID        NOT NULL REFERENCES subjects (subject_id),
    topic_id              UUID        REFERENCES topics (topic_id),
    requested_by          UUID        NOT NULL REFERENCES users (user_id),
    document_ids          JSONB       NOT NULL,
    bloom_distribution    JSONB       NOT NULL,
    total_questions       INTEGER     NOT NULL,
    lecturer_note         TEXT,
    retrieved_chunk_ids   JSONB,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_count       INTEGER     NOT NULL DEFAULT 0,
    rejected_count        INTEGER     NOT NULL DEFAULT 0,
    attempt_count         INTEGER     NOT NULL DEFAULT 0,
    issues_json           JSONB,
    error_message         TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version               BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_qgen_status CHECK (status IN ('PENDING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    CONSTRAINT ck_qgen_counts CHECK (total_questions > 0 AND generated_count >= 0
        AND rejected_count >= 0 AND attempt_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_qgen_subject_created
    ON question_generation_requests (subject_id, created_at DESC);

ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS fk_qversions_generation_request;
ALTER TABLE question_versions ADD CONSTRAINT fk_qversions_generation_request
    FOREIGN KEY (generation_request_id) REFERENCES question_generation_requests (generation_request_id);
