# AIVES — Verification Report

**Date of Execution:** 2026-10-25  
**Master Specification:** `docs/AIVES_FINAL_TASKS.md`  
**Execution Environment:** Windows 11, Java 24 / Maven 3.9.11, Node.js / React 19 / Vite 5.4.11, Docker Desktop (PostgreSQL + pgvector)  

---

## 1. Executive Summary

| Verification Target | Command Executed | Result | Evidence / Details |
|---|---|---|---|
| **Backend Clean Test Suite** | `mvn -f BE/pom.xml -o clean test -q` | **608 PASSED (0 failures, 0 errors, 0 skipped)** | All 608 unit, use case, domain, controller, security, storage gateway, Hibernate metamodel, and 24 live PostgreSQL integration tests passed |
| **Frontend Production Build** | `npm --prefix FE run build` | **PASSED** | Vite build succeeded (`✓ built in 8.49s`, 1953 modules transformed, 0 errors) |
| **Hibernate Mapping Metamodel** | `HibernateMappingSmokeTest` | **PASSED** | Metamodel built offline with all 23 JPA entities including `SpeechSettingsJpaEntity` and `PgVectorType` |
| **File Storage Gateways** | `FileStorageGatewayTest` | **PASSED (2/2)** | Validated path-safe deletion inside storage root, preservation of unrelated files in temp-root, and Cloudinary SDK resource destruction |
| **Speech Provider Engine Check** | `SpeechGatewayAdapterTest` | **PASSED** | Strict JSON parsing of `engine_ready` verified against true/false/missing/whitespace |
| **Speech Settings Domain & Validation**| `SpeechSettingsServiceImplTest` | **PASSED (11/11)** | Validated language/voice defaults, backward-compatibility, PATCH omitted preservation, explicit clear, and honest rejection of unsupported English voices |
| **Knowledge Search & Splitter Reuse** | `KnowledgeSearchServiceImplTest` | **PASSED (10/10)** | Multi-chunk Unicode text splitting via `TextSplitterPort`, dimension enforcement (1536), port-based cleanup (`fileStoragePort.delete`), atomic rollback |
| **PostgreSQL Real Knowledge Storage** | `QuestionBankIntegrationTest` | **PASSED (11/11)** | Direct integration test verifying `createKnowledge` persists non-null `file_url`, valid `storage_key`, `READY` status, and 1536-dim chunks |
| **Turn Answer Scoped RAG** | `SubmitTurnAnswerUseCaseTest` | **PASSED** | Bounded subject document retrieval, similar chunk injection to `VivaEvaluationContext`, no bank reads on snapshotted attempts |
| **Lecturer Grading Score Audit** | `LecturerGradingServiceImplTest` | **PASSED** | Non-blank reason requirement, `"SCORE_OVERRIDDEN"` and `"OVERRIDE_CLEARED"` audit tracking via `QuestionBankAuditor` |
| **Isolated Schema Upgrade Regression**| `ExamAttemptSnapshotIntegrationTest` | **PASSED (9/9)** | Isolated schema upgrade (`v19_upgrade_iso`) verified V0..V19 bootstrap, V19 record insertion, V20..V24 incremental application, custom legacy voice `'giahuy'` preservation, JSONB, and uniqueness |

---

## 2. Detailed Verification by Functional Area

### 2.1 Area A: Speech Settings & Knowledge Search Splitter Reuse
- **Domain & DTO Additions**:
  - `sttLanguage`, `ttsLanguage`, `vietnameseVoice`, `englishVoice` added to `SpeechSettings`, `SpeechSettingsJpaEntity`, `SpeechSettingsPersistenceMapper`, `SpeechSettingsResult`, `UpdateSpeechSettingsRequest`.
  - Default values: `sttLanguage = "vi-VN"`, `ttsLanguage = "vi-VN"`, `vietnameseVoice = "maichi"`, `englishVoice = null`.
  - `SpeechSettingsServiceImpl`: Rejects unsupported English voice requests honestly with `INVALID_SPEECH_VOICE`. PATCH preserves existing `englishVoice` when omitted (`null`), clears to `null` when blank (`""`), and validates when non-blank.
- **Provider Availability**:
  - `SpeechGatewayAdapter.isProviderAvailable()` parses JSON using `objectMapper.readTree(body).path("engine_ready").asBoolean(false)`.
- **Database Migration V24**:
  - `BE/src/main/resources/db/migration/V24__speech_settings_languages.sql`:
    1. Adds columns nullable first without defaults.
    2. Backfills from legacy `locale` and `voice` (`COALESCE(NULLIF(vietnamese_voice, ''), voice, 'maichi')`).
    3. Applies column defaults (`'vi-VN'`, `'maichi'`) and `NOT NULL` constraints.
- **Knowledge Splitter Reuse & Storage Architecture**:
  - Injected `TextSplitterPort` and `FileStoragePort` into `KnowledgeSearchServiceImpl`.
  - Removed null `FileStoragePort` constructor fallback; all dependencies explicitly wired in `ApplicationUseCaseConfiguration.java`.
  - Raw content persisted via `fileStoragePort.store()`, populating real `fileUrl` and `storageKey` on `CourseDocument` (respecting `course_documents.file_url NOT NULL`).
  - Replaced application-layer filesystem I/O (`Files.deleteIfExists`) with `fileStoragePort.delete(storageKey)`.
  - Storage gateways (`LocalFileStorageGateway`, `CloudinaryStorageGateway`) register transaction synchronization hooks (`STATUS_ROLLED_BACK`) to compensate on commit/transaction failures.
  - Enforced exact 1536 vector embedding dimensions; throws `ErrorCode.AI_SERVICE_UNAVAILABLE` on mismatch.
  - Verified against real PostgreSQL container in `QuestionBankIntegrationTest.createKnowledgePersistsValidCourseDocumentAndChunksInPostgreSql`.

### 2.2 Area B: Scoped RAG, Score Audit & V19 Upgrade Test
- **Scoped RAG Retrieval**:
  - `SubmitTurnAnswerUseCaseImpl` retrieves active, ready subject documents (`CourseDocument::isReady`).
  - Queries `documentChunkRepository.searchSimilarChunks(...)` with `questionText + " " + transcriptText`.
  - Formats retrieved chunks into RAG context (`[Tài liệu: ...] ...`) within `VivaEvaluationContext`. Fallback to `null` if no chunks available.
  - Frozen question snapshot and rubric remain immutable; student transcript is strictly evaluated data.
- **Lecturer Grading Score Audit**:
  - `LecturerGradingServiceImpl`: Requires non-blank reason on `overrideScore(...)` (`INVALID_REQUEST` if blank).
  - Preserves AI score and feedback untouched.
  - In `gradeQuestion(...)`, any existing score override is cleared while recording `"OVERRIDE_CLEARED"` audit event with prior override score and reason.
  - Total override score records `"SCORE_OVERRIDDEN"` audit event with old/new values, reason, actor, and timestamp.
- **V19 -> V24 Schema Upgrade Regression Test**:
  - In `ExamAttemptSnapshotIntegrationTest`, updated `v19ToV23SchemaUpgradeVerification()` to test incremental V0..V19 bootstrapping, record insertion under V19, sequential application of V20..V24.
  - Seeded custom non-default voice `'giahuy'` under V20 and verified that V24 backfill preserves `'giahuy'` without overwriting it with `'maichi'`.
  - Verified column presence, JSONB extraction (`rubric_snapshot->'criteria'->0->>'name'`), and uniqueness constraint enforcement in isolated disposable schema `v19_upgrade_iso`.

### 2.3 Parent Architecture & Bean Composition
- `ApplicationUseCaseConfiguration.java`:
  - Framework-free application core preserved.
  - Full constructor `@Bean` wiring declared for `SubmitTurnAnswerUseCaseImpl` (13 arguments), `KnowledgeSearchServiceImpl` (7 arguments), and `LecturerGradingServiceImpl` (11 arguments).
  - Clean imports; zero Spring/JPA annotations in domain and application modules.

---

## 3. Test & Build Execution Output

### 3.1 Backend Test Results
```text
[INFO] Tests run: 608, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
- **Passing**: 608 tests across domain, application, adapters, web controllers, storage gateways, rate limit filters, security, Hibernate metamodel, and all 24 PostgreSQL + pgvector Testcontainers integration tests:
  - `ExamAttemptSnapshotIntegrationTest`: 9/9 passed (includes real V19 -> V24 schema upgrade)
  - `QuestionBankIntegrationTest`: 11/11 passed (includes real `createKnowledge` PostgreSQL persistence)
  - `AdminUserIntegrationTest`: 4/4 passed
  - `FileStorageGatewayTest`: 2/2 passed

### 3.2 Frontend Production Build Output
```text
> fe@0.0.0 build
> vite build

vite v5.4.11 building for production...
transforming...
✓ 1953 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                                         1.86 kB │ gzip:  0.95 kB
dist/assets/index-BqrSWl-5.css                         90.48 kB │ gzip: 13.61 kB
dist/assets/index-ndjNij4K.js                         287.64 kB │ gzip: 85.66 kB
✓ built in 8.49s
```

---

## 4. Honest Unresolved Items & Operational Constraints

1. **TTS Model Language Scope**:
   - The verified ZeroTTS local CPU container on port 8001 supports Vietnamese voices (8 voices, 48kHz WAV). English speech synthesis is not supported by the local model; English voice selection is rejected honestly via API with `INVALID_SPEECH_VOICE [1071]`.
2. **Production Database DDL Governance**:
   - Live production databases require manual migration execution (`psql -1 -v ON_ERROR_STOP=1 -f <script>`) for V20 through V24 sequentially within explicit atomic transactions after backup. Applications do not execute automatic DDL in production (`spring.jpa.hibernate.ddl-auto=none`).
