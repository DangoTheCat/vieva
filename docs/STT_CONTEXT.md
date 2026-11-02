# Exam terminology context for STT

When an exam is opened, a post-commit callback asynchronously uses the existing Gemini ChatModel configuration to extract terminology from the frozen question versions and reference answers. There is no context table or migration. Context is cached in the backend process for six hours, up to 256 exams. The exam question set cannot change after publication. Backend restart or eviction clears context; the next authorized active attempt lookup starts generation again. Simultaneous lookups share the same pending result. Failed or empty generation falls back to ordinary STT.

## Configuration

- Keep `vieva.ai.provider=gemini` (environment `VIEVA_AI_PROVIDER`) and the existing `OPENAI_*` Gemini-compatible gateway configuration.
- Set the same nonempty random `STT_CONTEXT_SECRET` on backend and STT. Compose forwards it from the root `.env`. Empty means the private lookup is disabled; speech recognition still works.
- STT uses `STT_CONTEXT_BACKEND_URL=http://backend:8080` in Docker; local default is `http://localhost:8080`.
- Rebuild/restart backend, frontend and STT to activate the changes. No database migration is needed.

The browser sends only `attemptId` alongside the existing JWT and language. STT fetches `GET /api/v1/internal/stt/attempts/{attemptId}/context` with the JWT and `X-STT-Context-Secret`. Backend verifies the attempt owner and active status. Nginx rejects browser access to this route. No prompt or reference answer is added to student API responses. Each socket keeps its own context; a missing context is retried every five seconds as audio arrives. Requests time out after two seconds and fall back to normal recognition.

Whisper receives at most 200 actual tokenizer tokens from the bounded terminology list, on both partial and final passes. The admin-selected language is preserved. Browser Web Speech fallback cannot use Whisper context.

## Verification

Backend: `mvn -Dtest=ExamSttContextServiceTest,ExamServiceImplTest,ExamSttContextControllerTest,ArchitectureTest test`

STT: `pytest -q test_ws.py` (model stubbed; no GPU/model download).

Frontend: `npm run build`.

For acoustic acceptance, collect identical Vietnamese/English mixed audio clips with manually transcribed ground truth. Run the real model with and without generated context using identical decoding settings. Compare correct terminology occurrences / total occurrences, and check insertions of terms absent from the recording. Automated mocks only verify wiring; they do not establish an accuracy improvement. No real audio/provider evaluation has been performed here.
