-- ============================================================================
-- V4: Content & catalog schema
-- Creates the 12 tables backing the newly added domain/JPA entities so that
-- Hibernate `ddl-auto=validate` can start successfully.
--   subjects, topics, lecturer_subjects, course_documents, document_chunks,
--   questions, question_versions, question_sources, rubrics, rubric_criteria,
--   speech_config_versions, audit_events
-- ============================================================================

-- pgvector extension is required for document_chunks.embedding vector(1536)
CREATE EXTENSION IF NOT EXISTS vector;

-- ─── SUBJECTS ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subjects (
    subject_id   UUID         PRIMARY KEY,
    subject_code VARCHAR(50)  NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    description  TEXT,
    credits      INTEGER      NOT NULL DEFAULT 3,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_subjects_code UNIQUE (subject_code)
);

-- ─── TOPICS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS topics (
    topic_id    UUID         PRIMARY KEY,
    subject_id  UUID         NOT NULL REFERENCES subjects (subject_id) ON DELETE CASCADE,
    topic_name  VARCHAR(255) NOT NULL,
    order_index INTEGER      NOT NULL DEFAULT 1,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_topics_subject ON topics (subject_id);

-- ─── LECTURER_SUBJECTS (course-scoped RBAC) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS lecturer_subjects (
    lecturer_subject_id UUID        PRIMARY KEY,
    lecturer_id         UUID        NOT NULL REFERENCES users (user_id),
    subject_id          UUID        NOT NULL REFERENCES subjects (subject_id),
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by         UUID        NOT NULL REFERENCES users (user_id),
    revoked_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_lecturer_subjects_lookup
    ON lecturer_subjects (lecturer_id, subject_id, is_active);

-- Only one ACTIVE assignment per (lecturer, subject); revoked rows are free to be re-created.
CREATE UNIQUE INDEX IF NOT EXISTS uq_lecturer_subject
    ON lecturer_subjects (lecturer_id, subject_id)
    WHERE revoked_at IS NULL;

-- ─── COURSE_DOCUMENTS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_documents (
    document_id            UUID          PRIMARY KEY,
    subject_id             UUID          NOT NULL REFERENCES subjects (subject_id),
    uploaded_by            UUID          NOT NULL REFERENCES users (user_id),
    file_name              VARCHAR(255)  NOT NULL,
    file_url               VARCHAR(1000) NOT NULL,
    file_size_bytes        BIGINT        NOT NULL,
    mime_type              VARCHAR(100)  NOT NULL,
    indexing_status        VARCHAR(30)   NOT NULL DEFAULT 'UPLOADED',
    error_message          TEXT,
    extracted_text_summary TEXT,
    total_chunks           INTEGER       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_subject_status
    ON course_documents (subject_id, indexing_status);

-- ─── DOCUMENT_CHUNKS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document_chunks (
    chunk_id      UUID         PRIMARY KEY,
    document_id   UUID         NOT NULL REFERENCES course_documents (document_id) ON DELETE CASCADE,
    chunk_index   INTEGER      NOT NULL,
    content       TEXT         NOT NULL,
    token_count   INTEGER      NOT NULL,
    vector_id     VARCHAR(100),
    embedding     vector(1536),
    metadata_json JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_chunk_idx UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_chunks_doc ON document_chunks (document_id);

-- ─── QUESTIONS ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS questions (
    question_id   UUID        PRIMARY KEY,
    topic_id      UUID        NOT NULL REFERENCES topics (topic_id),
    question_code VARCHAR(50) NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by    UUID        NOT NULL REFERENCES users (user_id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_questions_code UNIQUE (question_code)
);

CREATE INDEX IF NOT EXISTS idx_questions_topic_status ON questions (topic_id, status);

-- ─── QUESTION_VERSIONS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_versions (
    question_version_id UUID        PRIMARY KEY,
    question_id         UUID        NOT NULL REFERENCES questions (question_id) ON DELETE CASCADE,
    version_number      INTEGER     NOT NULL DEFAULT 1,
    question_content    TEXT        NOT NULL,
    reference_answer    TEXT        NOT NULL,
    bloom_level         VARCHAR(20) NOT NULL,
    generation_mode     VARCHAR(20) NOT NULL,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reviewed_by         UUID        REFERENCES users (user_id),
    reviewed_at         TIMESTAMPTZ,
    rejection_reason    TEXT,
    created_by          UUID        NOT NULL REFERENCES users (user_id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_question_version UNIQUE (question_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_qversions_lookup ON question_versions (question_id, approval_status);

-- ─── QUESTION_SOURCES (RAG provenance) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_sources (
    question_source_id  UUID          PRIMARY KEY,
    question_version_id UUID          NOT NULL REFERENCES question_versions (question_version_id) ON DELETE CASCADE,
    chunk_id            UUID          NOT NULL REFERENCES document_chunks (chunk_id),
    citation_quote      TEXT          NOT NULL,
    similarity_score    NUMERIC(4, 3),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_question_version_chunk UNIQUE (question_version_id, chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_question_sources_version
    ON question_sources (question_version_id);

-- ─── RUBRICS ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rubrics (
    rubric_id           UUID          PRIMARY KEY,
    question_version_id UUID          NOT NULL,
    rubric_name         VARCHAR(255)  NOT NULL,
    total_points        NUMERIC(5, 2) NOT NULL,
    description         TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rubric_question_version UNIQUE (question_version_id),
    CONSTRAINT fk_rubrics_question_version
        FOREIGN KEY (question_version_id) REFERENCES question_versions (question_version_id) ON DELETE CASCADE
);

-- ─── RUBRIC_CRITERIA ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rubric_criteria (
    criterion_id            UUID          PRIMARY KEY,
    rubric_id               UUID          NOT NULL REFERENCES rubrics (rubric_id) ON DELETE CASCADE,
    criterion_name          VARCHAR(255)  NOT NULL,
    max_points              NUMERIC(5, 2) NOT NULL,
    achievement_descriptors TEXT          NOT NULL,
    order_index             INTEGER       NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_criteria_rubric ON rubric_criteria (rubric_id);

-- ─── SPEECH_CONFIG_VERSIONS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS speech_config_versions (
    config_version_id      UUID         PRIMARY KEY,
    version_code           VARCHAR(50)  NOT NULL,
    language_locale        VARCHAR(20)  NOT NULL,
    stt_provider           VARCHAR(50)  NOT NULL,
    tts_provider           VARCHAR(50)  NOT NULL,
    tts_voice_code         VARCHAR(50)  NOT NULL,
    tts_speech_rate        NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    tts_pitch              NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    is_active              BOOLEAN      NOT NULL DEFAULT FALSE,
    test_status            VARCHAR(20)  NOT NULL DEFAULT 'NOT_TESTED',
    tested_at              TIMESTAMPTZ,
    test_sample_transcript TEXT,
    created_by             UUID         NOT NULL REFERENCES users (user_id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_speech_config_version_code UNIQUE (version_code)
);

-- At most ONE active configuration per language (atomic activation guard).
CREATE UNIQUE INDEX IF NOT EXISTS uq_speech_config_active
    ON speech_config_versions (language_locale)
    WHERE is_active;

-- ─── AUDIT_EVENTS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_events (
    audit_id        UUID         PRIMARY KEY,
    actor_id        UUID         REFERENCES users (user_id),
    action_type     VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    old_values_json JSONB,
    new_values_json JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_events_lookup
    ON audit_events (entity_type, entity_id, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events (actor_id);
