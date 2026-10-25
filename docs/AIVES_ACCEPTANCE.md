# Codex independent acceptance — 2026-10-25

This is the current acceptance record and supersedes earlier intermediate review/progress documents. No commit, push, deployment or migration of the user's existing database was performed.

## Accepted behavior

- Only ADMIN creates subjects and assigns/revokes lecturers. Admin controllers and SecurityConfig enforce ADMIN; lecturer catalog exposes assigned subjects and topic operations, no subject-creation endpoint. Head assignment endpoint, active head domain/DTO/mapping/UI and obsolete services/test removed. Historical head database column remains unused; V0..V19 unchanged.
- Two Gemini Flash coding workers implemented bounded corrections; Codex reviewed their source, rejected earlier incomplete passes, and re-reviewed fixes. The later final-gap batch reused two workers for speech/knowledge and RAG/grading/upgrade, with shared wiring integrated outside core.
- Knowledge creation uses shared TextSplitterPort, actual storage, subject/topic authorization, 1536 embeddings, transaction boundaries and storage-port cleanup. Local and Cloudinary adapters own IO/rollback cleanup; application no longer deletes raw filesystem paths.
- Templates copy validated actual criteria into authorized same-subject drafts. Text import and document topic updates preserve the existing question version/approval lifecycle and cited evidence.
- Exams freeze ordered questions/answers/rubrics, serialize entry with a database lock and uniqueness constraint, and use snapshots during answers. Ownership/assignment checks, answer replay, post-commit finalization, main-question group grading and teacher/AI separation were reviewed. RAG uses ready/nondeleted documents of the attempt subject, bounded top3 retrieval.
- Score overrides require a reason; grading/override/clearing events retain old/new values through the existing transactional auditor. Effective totals remain consistent on reads.
- Speech exposes separate STT/TTS languages and Vietnamese/English voice configuration with legacy compatibility. Unsupported English synthesis is rejected; no English model or fake voice was added. V24 preserves a nondefault legacy voice before setting defaults.

## Independent verification

- Parsed the final clean Maven XML reports: **608 tests, 0 failures, 0 errors, 0 skipped**. **24** real PostgreSQL/pgvector integration cases across AdminUserIntegrationTest (4), ExamAttemptSnapshotIntegrationTest (9), QuestionBankIntegrationTest (11).
- Inspected integration-test source: concurrent entry returns one attempt, native JSONB roundtrip, rollback and finalization, replay after completion, real knowledge persistence, and isolated V0..V19 bootstrap plus existing-data upgrade to V24 (including nondefault giahuy voice preservation).
- Inspected FileStorageGatewayTest: local file deletion under temporary root preserves unrelated file; Cloudinary deletion uses raw-resource public ID. Reviewed rollback hooks and knowledge failure cleanup.
- Frontend production build passed after removal of head UI; the final storage-only change does not alter FE source. Final Gemini build result is also recorded in AIVES_VERIFICATION.md.
- Independently verified running ZeroTTS Docker HTTP readiness/voices and host WAV artifact. Real model runs **CPU**, 8 Vietnamese voices, **48000Hz**. Gemini executed **22** in-container unit/router tests and live POST/GET WAV synthesis. This is separate from mock LLM testing.
- No active head/self-create symbol remains in BE/main or FE/src. Domain/application import scan found no Spring, JPA, infrastructure or web imports. Historical SQL changes were checked independently.

## Architecture review

Seven diagnostic checks satisfied for the changed paths: testable business logic, inward dependencies, persistence behind ports, delivery-independent use cases, framework confined outside core, no introduced dependency cycle in reviewed paths, infrastructure composition-root wiring. **9/10**: remaining service/query size and compatibility constructors are maintainability concerns, not acceptance blockers. No full-repository rewrite was attempted.

## Practical limits and handoff

- Live LLM/STT external provider quality was not tested; automated AI tests use deterministic mocks. Only ZeroTTS real model was installed, as requested.
- Existing user database still requires explicit backed-up incremental migration V20..V24. See AIVES_HANDOFF.md. Do not rerun historical V2/V0..V19; the handoff uses an explicit script list.
- Source and verification are delivered uncommitted in the current working tree. See AIVES_HANDOFF.md for APIs and AIVES_VERIFICATION.md for commands/results. Earlier AIVES_REVIEW*.md describe historical findings and are not the final state.
