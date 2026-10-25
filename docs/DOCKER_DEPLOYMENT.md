# Backend and database Docker delivery

Verified 2026-10-26 (Asia/Saigon). Gemini Flash workers `afd510a1-faa1-4b08-87e3-30224e195056` (backend image/readiness) and `858be5c7-8b1e-4702-8f4a-a27f84b63daa` (Compose/docs) implemented the initial changes. Codex reviewed and corrected DB healthcheck variables, readiness boundaries/wiring, smoke failure handling, and host/container TTS URL separation.

## Run

Create your private `.env` from `.env.example` and set DB_PASSWORD/JWT_SECRET. Never commit `.env`.

```powershell
docker compose up -d --build postgres backend
docker compose ps
powershell -NoProfile -File scripts/docker-smoke.ps1
```

Backend: http://localhost:8080. Readiness: `/api/v1/health/readiness` verifies a DB connection and returns 503 when unavailable. `/api/v1/health` remains process liveness. PostgreSQL is bound to localhost:5432. Both run in Docker; no host Java/PostgreSQL process is needed.

The database uses `postgres_data`, uploaded documents use `backend_documents`. Stop without removing volumes. Never use `down -v` to upgrade. Fresh PostgreSQL volumes execute the versioned SQL bootstrap. Existing volumes do not auto-upgrade: back up and apply only missing migrations using the handoff instructions; no automatic Flyway migration was added.

Optional profiles: `frontend`, `tts` (ZeroTTS ONNX CPU), `stt-gpu` (NVIDIA required), `full` (includes GPU STT). Default startup launches only BE and DB. TTS model cache uses `tts_model_cache`.

The backend container uses `DB_HOST=postgres` and `TTS_BASE_URL=http://tts-service:8001`. Override the container TTS endpoint through `DOCKER_TTS_BASE_URL`; host development uses `TTS_BASE_URL` separately. Optional TTS is not started automatically and speech operations may be unavailable until it is started.

## Evidence

- Built and started `vieva-backend` (Java 17 nonroot runtime) and `vieva-postgres` (PostgreSQL16/pgvector): both healthy.
- Compose configuration validation and PowerShell smoke passed against the final running containers.
- HTTP readiness: status UP, database UP.
- Seed-admin login and authenticated `GET /api/v1/admin/users?page=0&size=1` passed, total users4. JWT not logged or saved.
- Final Maven Wrapper `clean test`: **618 tests, 0 failures, 0 errors, 0 skipped**, including disposable PostgreSQL integration tests. Surefire emitted a fork shutdown timeout warning after successful tests; Maven exited0. No assertions or tests were weakened.
- Existing user DB volumes were not reset or migrated. Local Docker stack is left running. Secrets, generated logs and model/audio files are not committed.
