# AIVES — Technical Handoff Document

**Date:** 2026-10-25  
**Version:** Production Gap Closure Release (V24)  
**System:** AI-based Voice Interview and Evaluation System (AIVES)  

---

## 1. System Overview & Architecture

AIVES is structured as a **Modular Monolith** adhering strictly to **Clean Architecture**:
- **Domain Layer (`com.example.vieva.domain`)**: Pure Java entities, value objects, and domain invariants. Zero dependencies on Spring or Jakarta persistence annotations.
- **Application Layer (`com.example.vieva.application`)**: Input/output ports and pure use case implementations. Framework-free business orchestration; no Spring `@Autowired` or `@Service` annotations. Transaction demarcation is expressed via the pure custom `@UseCaseTransaction` annotation.
- **Infrastructure & Adapters (`com.example.vieva.infrastructure`, `com.example.vieva.adapters`)**: Composition root in `ApplicationUseCaseConfiguration.java` using explicit `@Bean` constructor injection. Persistence repositories, HTTP controllers, and external service gateways (Tika, LangChain4j, ZeroTTS, STT, Local & Cloudinary Storage) live entirely in this boundary.
- **Authoritative Workflow Rules**: Administrator creates and assigns subjects (`AdminSubjectController`, `AdminLecturerSubjectController`). There is no lecturer self-creation or subject head review tier.

---

## 2. API Endpoints & Request Contracts

### 2.1 Speech Settings Management (UC7.2)
- **`GET /api/v1/admin/speech`**
  - **Authorization**: `ROLE_ADMIN`
  - **Response (`200 OK`)**:
    ```json
    {
      "id": "00000000-0000-0000-0000-000000000001",
      "locale": "vi-VN",
      "voice": "maichi",
      "speed": 1.00,
      "pitch": 1.00,
      "sttLanguage": "vi-VN",
      "ttsLanguage": "vi-VN",
      "vietnameseVoice": "maichi",
      "englishVoice": null,
      "updatedBy": "00000000-0000-0000-0000-000000000002",
      "updatedAt": "2026-10-25T06:05:00Z"
    }
    ```
- **`PATCH /api/v1/admin/speech`**
  - **Authorization**: `ROLE_ADMIN`
  - **Request Body**:
    ```json
    {
      "sttLanguage": "vi-VN",
      "ttsLanguage": "vi-VN",
      "vietnameseVoice": "maichi",
      "englishVoice": null
    }
    ```
  - **Behavior & Validation**:
    - Validates gateway availability via JSON parsing (`{"engine_ready": true}`) on `/health`.
    - **PATCH Semantics for `englishVoice`**:
      - If omitted (`null`): Preserves existing saved `englishVoice`.
      - If blank (`""`): Explicitly clears `englishVoice` to `null`.
      - If candidate string: Validates against speech gateway capability (`isVoiceSupported("en-US", candidate)`). Rejects honestly with HTTP 400 (`ErrorCode.INVALID_SPEECH_VOICE [1071]`) if unsupported.
    - Legacy payloads omitting language fields maintain bidirectional synchronization with `locale` and `voice`.
- **`GET /api/v1/student/speech`**
  - **Authorization**: Authenticated student
  - **Response**: Read-only `SpeechSettingsResult` matching admin configuration.

### 2.2 Knowledge Search & Storage Pipeline (UC1.1)
- **Endpoint**: `POST /api/v1/knowledge`
- **Text Processing**: Injects shared `TextSplitterPort` to chunk input text via `ParsedSection` and `TextChunk`, guaranteeing exact token bounding and Unicode substring integrity.
- **Persistence & URL Storage**: Injects `FileStoragePort` to store raw text (`fileStoragePort.store(...)`). Stores real `StoredFile` URL and storage key in `CourseDocument`, strictly satisfying `course_documents.file_url NOT NULL`.
- **Vector Embedding Validation**: Enforces exact configured 1536-dimensional vectors (`embedding == null || embedding.length != 1536`), throwing `ErrorCode.AI_SERVICE_UNAVAILABLE`.
- **Port-Based Cleanup & Rollback Compensation**:
  - The use case contains zero direct filesystem I/O (`Files.deleteIfExists` removed). Upon failure, it invokes `fileStoragePort.delete(storageKey)`.
  - In infrastructure, `LocalFileStorageGateway` and `CloudinaryStorageGateway` register transaction synchronization hooks (`afterCompletion`) on active transactions, deleting uncommitted files if a transaction rolls back (including commit-phase failures).
  - `LocalFileStorageGateway` uses path-safe `resolve(key)` ensuring no deletion outside the storage root. `CloudinaryStorageGateway` destroys raw resources via the Cloudinary SDK.

### 2.3 Scoped RAG in Oral Exam Turn Answering (UC3.4)
- **Endpoint**: `POST /api/v1/student/exam-attempts/{attemptId}/turns/{turnId}/answer`
- **Scoped Context Retrieval**:
  - Automatically queries active, non-deleted, indexed documents (`indexingStatus == READY`) belonging strictly to the attempt's `subjectId`.
  - Computes cosine similarity of `(questionText + " " + studentAnswer)` against `document_chunks`.
  - Formats retrieved chunks into RAG context (`[Tài liệu: ...] ...`) within `VivaEvaluationContext`.
  - Student transcript is treated strictly as data input; frozen snapshot questions and rubrics are immutable.
  - Falls back cleanly to `ragContext: null` when no subject documents are available or chunk similarity is below threshold.

### 2.4 Lecturer Grading & Score Audit Trail (WF03)
- **`POST /api/v1/lecturer/grading/attempts/{attemptId}/turns/{turnId}`**
  - Grades individual question turn (`teacher_score`, `teacher_feedback`).
  - Records `"QUESTION_GRADED"` audit event via `QuestionBankAuditor`.
  - **Override Clearing Invariant**: If a manual total score override was previously recorded on the attempt, regrading any turn clears `overrideTotalScore` and records `"OVERRIDE_CLEARED"` in audit history with prior override score and reason.
- **`POST /api/v1/lecturer/grading/attempts/{attemptId}/override`**
  - **Request Body**:
    ```json
    {
      "overrideTotalScore": 8.75,
      "reason": "Corrected for background noise interference during turn 2"
    }
    ```
  - **Validation**: Requires non-blank `reason` (`ErrorCode.INVALID_REQUEST [1006]`).
  - **Behavior**: Preserves AI score and AI feedback untouched. Recalculates effective grade and records `"SCORE_OVERRIDDEN"` audit log.

---

## 3. Database Migration & Schema Upgrade Instructions

### 3.1 Migration File Inventory (V20 through V24)
- `V0` through `V19`: Core baseline schema (users, roles, subjects, topics, documents, chunks, vectors, questions, rubrics, attempts, turns, timers).
- `V20`: Speech settings table creation and default seed.
- `V22`: Exam definitions, turn snapshots, partial unique index `(exam_id, student_id)`.
- `V23`: Teacher grading and score audit columns (`teacher_score`, `override_total_score`, `override_reason`, etc.).
- `V24__speech_settings_languages.sql`: 3-stage safe migration:
  1. Adds `stt_language`, `tts_language`, `vietnamese_voice`, `english_voice` as nullable without defaults.
  2. Backfills existing rows from legacy `locale` and `voice` (`COALESCE(NULLIF(vietnamese_voice, ''), voice, 'maichi')`), preserving any custom legacy voices (e.g., `'giahuy'`).
  3. Applies column defaults (`'vi-VN'`, `'maichi'`) and `NOT NULL` constraints.

### 3.2 Production Manual Upgrade Procedure (V19 -> V24)
> [!IMPORTANT]
> AIVES does not execute automatic DDL migrations against live production databases (`spring.jpa.hibernate.ddl-auto=none`).
> To upgrade a production database from V19 to V24, operators must apply migrations V20 through V24 sequentially.
> Each script MUST be executed with `psql -1` (or `--single-transaction`) to wrap the script in an atomic `BEGIN ... COMMIT` block. Using `psql -f` without `-1` does NOT run the file in a transaction.

```bash
# Step 1: Backup production database before schema changes
pg_dump -Fc -h localhost -U postgres -d vieva_db > /backup/vieva_db_pre_v20_to_v24_$(date +%F).dump

# Step 2: Apply migrations V20 through V24 sequentially, each in an explicit atomic transaction (-1)
for script in \
    BE/src/main/resources/db/migration/V20__admin_speech_settings.sql \
    BE/src/main/resources/db/migration/V21__content_and_rubric_templates.sql \
    BE/src/main/resources/db/migration/V22__create_exams_and_snapshots_schema.sql \
    BE/src/main/resources/db/migration/V23__add_teacher_grading_and_override_audit.sql \
    BE/src/main/resources/db/migration/V24__speech_settings_languages.sql; do
    echo "Applying: $script"
    psql -1 -v ON_ERROR_STOP=1 -h localhost -U postgres -d vieva_db -f "$script"
done

# Step 3: Verify upgraded columns and legacy voice preservation
psql -h localhost -U postgres -d vieva_db -c "SELECT id, locale, voice, stt_language, tts_language, vietnamese_voice, english_voice FROM speech_settings;"
```

---

## 4. Local Port & Service Topology

| Service | Host Port | Technology / Details |
|---|---|---|
| **Backend API** | `8080` | Spring Boot 3.4.3 (Java 24 / 17), Modular Monolith |
| **Frontend Web** | `3000` / `5173` | React 19, Vite 5.4, Tailwind CSS, Neo-Brutalist UI |
| **PostgreSQL + pgvector** | `5432` | `pgvector/pgvector:pg16` |
| **AI STT Service** | `8002` | `faster-whisper` (`vi-whisper-large-v3-turbo`) |
| **AI TTS Service** | `8001` | ZeroTTS container (8 Vietnamese voices, 48kHz WAV) |

---

## 5. Verification Summary & Known Limitations

- **Backend Test Suite**: **608 passing tests** (0 failures, 0 errors, 0 skipped) across unit, slice, JPA metamodel, and 24 live PostgreSQL integration tests.
- **Frontend Production Build**: `npm --prefix FE run build` completes cleanly in 7.36s with 0 errors (1953 modules transformed).
- **Offline / Mock Mode**: Standard test suites run with `vieva.ai.provider=mock` and `vieva.mail.provider=log` for fast deterministic execution. Live integration tests spin up isolated disposable `pgvector/pgvector:pg16` Testcontainers.
- **Known Limitations**:
  - Real ZeroTTS engine currently supports Vietnamese voices (`maichi` default). English voice synthesis is not supported by the local ZeroTTS model; English voice selection is rejected honestly via API with `INVALID_SPEECH_VOICE [1071]`.
  - Testcontainers integration tests require an accessible Docker daemon. When Docker is active, all 24 database integration tests pass completely.
