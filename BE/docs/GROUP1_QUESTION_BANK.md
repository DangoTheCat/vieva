# Nhóm 1 — Ngân hàng câu hỏi & Rubric (UC1.1 – UC1.7, WF01)

Tài liệu bàn giao backend cho nhóm chức năng 1 của AIVES: kiến trúc, cách chạy, API, mã lỗi, giả định.

## 1. Tổng quan theo pha

| Pha | Nội dung | Thành phần chính |
|-----|----------|------------------|
| 0 | Rà repo | Đã có sẵn entity/migration V4–V6; thiếu `subject_id`, con trỏ bản duyệt, `bloom_confirmed`, `parent_version_id`, ngữ cảnh sinh câu hỏi, HNSW, CHECK, storage đọc lại được, chunk overlap, import, audit, kiểm quyền trong transaction |
| 1 | Schema + domain | `V7__group1_question_bank.sql`, `BloomLevel` (6 mức, nhãn tiếng Việt), `BloomDistribution`, `QuestionGenerationRequest`, state machine trong `CourseDocument`, `Question`, `QuestionVersion`, `Rubric`, `RubricCriterion` |
| 2 | UC1.5, UC1.7, UC1.3, UC1.4 + Copy-on-Write | `QuestionAuthoringService`, `RubricService`, `QuestionReviewService`, `QuestionBankService`, `QuestionApprovalPolicy` |
| 3 | UC1.6 import | `QuestionImportService`, `PoiCsvQuestionSpreadsheetGateway` (xlsx/csv, dry-run) |
| 4 | UC1.1 upload + index bất đồng bộ | `DocumentIndexingService`, `TikaDocumentParserGateway` (trang/slide/heading), `TokenTextSplitterGateway` (800/100 token), `LocalFileStorageGateway`, retry giới hạn |
| 5 | UC1.2 RAG + sinh lại từng câu | `QuestionGenerationService`, `GeneratedQuestionValidator`, `OpenAiQuestionGenerationGateway`, `MockQuestionGenerationGateway`, `MockEmbeddingGateway` |
| 6 | Audit, rà BR, test | `QuestionBankAuditor` (BR-09), `SubjectAccessGuard` (BR-06), 181 test |

Luồng phụ thuộc: `adapters (controller/presenter)` → `application (use case, port, DTO)` → `domain (entity, policy)`; `infrastructure` hiện thực port (JPA, Tika, POI, Spring AI, storage).

## 2. Business rules — nơi được bảo đảm

| Rule | Hiện thực |
|------|-----------|
| BR-01 Bloom 6 mức, AI phải được GV xác nhận | Enum + CHECK `ck_qversions_bloom_level`; `bloom_confirmed=false` cho bản AI; `QuestionApprovalPolicy` chặn `BLOOM_NOT_CONFIRMED` |
| BR-02 Σ max_score = total | `Rubric.recalculateTotal`, `RubricDraftFactory`, validator đầu ra LLM, policy khi duyệt |
| BR-03 Câu AI phải có nguồn có căn cứ | Validator: nguồn ∈ chunk đã truy hồi + trích dẫn nguyên văn; policy: chunk còn tồn tại, tài liệu READY, trích dẫn khớp |
| BR-04 Không xoá cứng bằng chứng | FK `ON DELETE CASCADE` → RESTRICT (V7); chỉ xoá DRAFT; tài liệu đang được trích dẫn → `DOCUMENT_IN_USE`; ARCHIVED thay vì xoá |
| BR-05 Chỉ UC1.3 đưa vào ngân hàng | Chỉ `QuestionReviewService.approve` gọi `Question.publish`; tìm kiếm ngân hàng lọc `current_approved_version_id IS NOT NULL` |
| BR-06 Kiểm quyền môn mỗi request | `@PreAuthorize(@courseSecurityEvaluator...)` + `SubjectAccessGuard` chạy lại **trong** transaction ghi (mất quyền giữa chừng → `FORBIDDEN_SUBJECT`, không lưu gì) |
| BR-07 Chỉ READY, lọc đúng môn/tài liệu | Native query lọc `subject_id`, `document_id IN (...)`, `indexing_status='READY'`, `deleted_at IS NULL` |
| BR-08 Duyệt nguyên tử + optimistic lock | `approve` một transaction; `@Version` trên `question_versions`, `questions`, `question_generation_requests`; client gửi `expectedVersion` (= `lockVersion` đã đọc) |
| BR-09 Audit | `audit_events`: DOCUMENT_UPLOADED / INDEXED / INDEX_FAILED / REINDEX_REQUESTED / DELETED, QUESTIONS_GENERATED, QUESTION_REGENERATED, QUESTION_CREATED, QUESTION_DRAFT_UPDATED, QUESTION_BLOOM_CONFIRMED, RUBRIC_UPDATED, QUESTION_VERSION_CREATED, QUESTION_DRAFT_DELETED, QUESTION_APPROVED, QUESTION_REJECTED, QUESTION_ARCHIVED, QUESTIONS_IMPORTED, TOPIC_CREATED |

## 3. Chạy migration và test

Migration chạy tự động bằng Flyway khi khởi động (`spring.flyway.enabled=true`). V7 tương thích dữ liệu cũ: backfill `subject_id`, `current_approved_version_id`, `document_id`, map Bloom 4 mức cũ (`NHAN_BIET`→`REMEMBER`, `THONG_HIEU`→`UNDERSTAND`, `VAN_DUNG`→`APPLY`, `VAN_DUNG_CAO`→`ANALYZE`).

```bash
docker compose up -d postgres
```

```bash
cd BE && ./mvnw spring-boot:run
```

Toàn bộ test (integration test cần Docker đang chạy; tự kéo image `pgvector/pgvector:pg16`):

```bash
cd BE && ./mvnw test
```

Chỉ integration test:

```bash
cd BE && ./mvnw test -Dtest=QuestionBankIntegrationTest
```

`src/test/resources/docker-java.properties` ép `api.version=1.44` vì Docker Engine 29 từ chối API mặc định của Testcontainers 1.20.x.

### Biến môi trường mới

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `VIEVA_AI_PROVIDER` | `mock` | `mock` (offline, dev/test) hoặc `openai` (cần `OPENAI_API_KEY`) |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Model sinh câu hỏi |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Phải ra 1536 chiều |
| `VIEVA_STORAGE_PROVIDER` | `local` | `local` hoặc `cloudinary` (cần `CLOUDINARY_*`) |
| `VIEVA_STORAGE_ROOT` | `./data/documents` | Thư mục lưu file khi `local` |

Các ngưỡng khác (`vieva.rag.*`, `vieva.documents.*`, `vieva.import.*`) nằm trong `application.properties`.

## 4. API (role LECTURER hoặc ADMIN, prefix `/api/v1/lecturer`)

Mọi lỗi có dạng:

```json
{"code":"1034","error":"RUBRIC_SCORE_MISMATCH","message":"...","errors":[{"field":"rubric.totalScore","error":"RUBRIC_SCORE_MISMATCH","message":"..."}],"timestamp":"..."}
```

| Method & path | UC | Kết quả |
|---------------|----|---------|
| `GET /subjects` | — | Môn được phân công |
| `GET /subjects/{id}/topics`, `POST /subjects/{id}/topics` | — | Chủ đề (tạo tối thiểu) |
| `POST /subjects/{id}/documents` (multipart `file`) | 1.1 | **202**, trạng thái UPLOADED |
| `GET /subjects/{id}/documents`, `GET /documents/{id}` | 1.1 | Theo dõi trạng thái |
| `POST /documents/{id}/retry-index` | 1.1 | **202**, FAILED → UPLOADED (tối đa `max-index-attempts`) |
| `DELETE /documents/{id}` | 1.1 | Soft delete; 409 nếu đang được trích dẫn |
| `POST /question-generation-requests` | 1.2 | **201**, báo cáo + bản nháp |
| `GET /question-generation-requests/{id}`, `POST .../{id}/retry` | 1.2 | Xem / sinh bù câu còn thiếu |
| `POST /question-versions/{id}/regenerate` | 1.2 | Sinh lại đúng 1 bản nháp AI |
| `GET /question-versions?subjectId=&status=DRAFT&origin=&bloomLevel=&topicId=&generationRequestId=&keyword=` | 1.3 | Hàng đợi duyệt |
| `GET/PUT/DELETE /question-versions/{id}` | 1.3 | Xem / sửa / xoá bản DRAFT |
| `POST /question-versions/{id}/confirm-bloom` | 1.3 | Xác nhận Bloom (BR-01) |
| `POST /question-versions/{id}/approve`, `.../reject` | 1.3 | Duyệt (transaction) / từ chối kèm lý do |
| `GET/PUT /question-versions/{id}/rubric`, `POST .../rubric/criteria`, `PUT/DELETE .../rubric/criteria/{criterionId}` | 1.7 | Rubric của DRAFT, tự tính total |
| `GET /subjects/{id}/questions?topicId=&bloomLevel=&status=&keyword=&page=&size=&sortBy=&sortDir=` | 1.4 | Ngân hàng (chỉ câu có bản APPROVED) |
| `GET /questions/{id}`, `GET /questions/{id}/versions` | 1.4 | Chi tiết, lịch sử phiên bản |
| `POST /questions/{id}/versions` | 1.3 | Copy-on-Write → DRAFT mới |
| `POST /questions/{id}/archive` | 1.4 | ACTIVE → ARCHIVED |
| `POST /subjects/{id}/questions` | 1.5 | **201** DRAFT MANUAL |
| `GET /import-templates/questions?format=xlsx\|csv` | 1.6 | Tải template |
| `POST /subjects/{id}/questions/import?dryRun=true\|false` (multipart `file`) | 1.6 | Báo cáo theo dòng/cột |

### Ví dụ

**Upload tài liệu** — `POST /subjects/{subjectId}/documents` → `202`

```json
{"documentId":"7c1e...","subjectId":"3f2a...","fileName":"Giao trinh CSDL.pdf","mimeType":"application/pdf",
 "indexingStatus":"UPLOADED","totalChunks":0,"indexAttempts":0}
```

Sau vài giây `GET /documents/{id}` → `"indexingStatus":"READY","totalChunks":42` hoặc `"FAILED","errorMessage":"No text could be extracted from the document (empty or scanned without a text layer)"`.

**Sinh câu hỏi** — `POST /question-generation-requests`

```json
{"subjectId":"3f2a...","topicId":null,"documentIds":["7c1e..."],"totalQuestions":4,
 "bloomDistribution":{"REMEMBER":1,"APPLY":2,"EVALUATE":1},"lecturerNote":"Tập trung vào giao dịch ACID"}
```

→ `201`

```json
{"generationRequestId":"b0d9...","status":"PARTIAL","totalQuestions":4,"generatedCount":3,"rejectedCount":2,"attemptCount":3,
 "issues":["Attempt 1, item 2: rubric criteria sum (9) differs from totalScore (10)",
           "Attempt 2, item 1: citation for C3 is not found verbatim in the chunk"],
 "drafts":[{"versionId":"e41c...","status":"DRAFT","origin":"AI_RAG","bloomLevel":"APPLY","bloomLevelLabel":"Vận dụng",
            "bloomConfirmed":false,"lockVersion":0,
            "rubric":{"totalScore":10,"criteria":[{"name":"Độ chính xác","maxScore":6},{"name":"Lập luận","maxScore":4}]},
            "sources":[{"chunkId":"9aa1...","documentName":"Giao trinh CSDL.pdf","citationQuote":"Tính nguyên tử bảo đảm...","similarityScore":0.482}]}]}
```

`status` = `COMPLETED` / `PARTIAL` / `FAILED`. Lỗi LLM khi chưa có câu hợp lệ nào → vẫn `201` với `"status":"FAILED"` và `errorMessage`; gọi `POST .../{id}/retry` (tối đa `max-total-attempts` lượt LLM cho mỗi request). Không có đoạn tài liệu liên quan → `422 INSUFFICIENT_CONTEXT`.

**Sinh lại một câu** — `POST /question-versions/{versionId}/regenerate`

```json
{"feedback":"Cần tình huống thực tế trong ngân hàng","expectedVersion":0}
```

→ `200` bản nháp đã thay nội dung, `regenerationCount` +1, `bloomConfirmed=false`. Không có ứng viên hợp lệ → `502 AI_SERVICE_UNAVAILABLE`, bản nháp giữ nguyên.

**Sửa bản nháp** — `PUT /question-versions/{versionId}`

```json
{"content":"Hãy áp dụng ACID cho giao dịch chuyển khoản?","bloomConfirmed":true,"expectedVersion":1,
 "rubric":{"criteria":[{"name":"Đúng tính chất","description":"0: sai; 3: một phần; 6: đủ","maxScore":6,
   "levels":[{"label":"Đạt","description":"Nêu đủ 4 tính chất","score":6}]},
   {"name":"Ví dụ","description":"0-4","maxScore":4}]}}
```

**Duyệt** — `POST /question-versions/{versionId}/approve` body tuỳ chọn `{"expectedVersion":2}`

- `200` → `"status":"APPROVED"`; bản APPROVED trước đó thành `SUPERSEDED`.
- `422` → `{"error":"BLOOM_NOT_CONFIRMED","errors":[{"field":"bloomConfirmed",...},{"field":"sources","error":"SOURCE_REQUIRED",...}]}`
- `409 VERSION_NOT_DRAFT`, `409 CONCURRENT_MODIFICATION`, `403` nếu đã mất phân công.

**Từ chối** — `POST /question-versions/{id}/reject` `{"reason":"Câu hỏi ngoài phạm vi chương 3"}`

**Tạo thủ công** — `POST /subjects/{subjectId}/questions`

```json
{"topicId":null,"content":"Trình bày 4 tính chất ACID","expectedAnswer":"Atomicity, Consistency, Isolation, Durability",
 "bloomLevel":"UNDERSTAND","rubric":{"name":"Rubric ACID","criteria":[{"name":"Đủ ý","description":"0-10","maxScore":10}]}}
```

**Import** — `POST /subjects/{subjectId}/questions/import?dryRun=true`

```json
{"dryRun":true,"totalRows":5,"totalQuestions":4,"validQuestions":2,"invalidQuestions":2,"createdQuestions":0,
 "errors":[{"row":4,"column":"bloom_level","questionRef":"Q2","message":"bloom_level 'X' must be one of REMEMBER/Nhớ, ..."}],
 "createdQuestionIds":[]}
```

Cột template: `question_ref, topic, content, expected_answer, bloom_level, criterion_name, criterion_description, criterion_max_score`. Một câu nhiều tiêu chí = nhiều dòng cùng `question_ref`. `bloom_level` nhận mã (`APPLY`) hoặc nhãn (`Vận dụng`, không phân biệt dấu/hoa thường). Topic chưa có sẽ được tạo khi commit.

**Ngân hàng** — `GET /subjects/{id}/questions?bloomLevel=ANALYZE&status=ACTIVE&keyword=giao%20dịch`

```json
{"content":[{"questionId":"...","questionCode":"Q-8F21C0A9D3","status":"ACTIVE","hasPendingDraft":false,
  "currentVersion":{"versionNumber":2,"status":"APPROVED","bloomLevel":"ANALYZE", "rubric":{...}, "sources":[...]}}],
 "page":0,"size":20,"totalElements":1,"totalPages":1}
```

### Mã lỗi nghiệp vụ

| HTTP | error |
|------|-------|
| 400 | `INVALID_REQUEST` (validation, có `errors[]`) |
| 403 | `UNAUTHORIZED` (sai role / không được phân công ở tầng HTTP), `FORBIDDEN_SUBJECT` (mất phân công trong transaction) |
| 404 | `SUBJECT_NOT_FOUND`, `TOPIC_NOT_FOUND`, `DOCUMENT_NOT_FOUND`, `QUESTION_NOT_FOUND`, `QUESTION_VERSION_NOT_FOUND`, `GENERATION_REQUEST_NOT_FOUND`, `CRITERION_NOT_FOUND` |
| 409 | `VERSION_NOT_DRAFT`, `DRAFT_ALREADY_EXISTS`, `NO_APPROVED_VERSION`, `QUESTION_ARCHIVED`, `CONCURRENT_MODIFICATION`, `DOCUMENT_NOT_READY`, `DOCUMENT_NOT_RETRYABLE`, `RETRY_LIMIT_EXCEEDED`, `DOCUMENT_IN_USE`, `REGENERATION_NOT_SUPPORTED`, `SUBJECT_INACTIVE` |
| 413 | `FILE_TOO_LARGE`, `IMPORT_LIMIT_EXCEEDED` |
| 415 | `UNSUPPORTED_FILE_TYPE` (đuôi không cho phép hoặc nội dung thật khác đuôi) |
| 422 | `RUBRIC_SCORE_MISMATCH`, `RUBRIC_REQUIRED`, `SOURCE_REQUIRED`, `INVALID_CITATION_QUOTE`, `BLOOM_NOT_CONFIRMED`, `CONTENT_REQUIRED`, `INSUFFICIENT_CONTEXT`, `EMPTY_DOCUMENT_TEXT`, `IMPORT_FILE_INVALID`, `INVALID_BLOOM_DISTRIBUTION`, `DOCUMENT_SUBJECT_MISMATCH` |
| 502 | `AI_SERVICE_UNAVAILABLE` |

## 5. Giả định đã áp dụng

1. Question chỉ xuất hiện ở UC1.4 khi có ≥1 bản APPROVED (`current_approved_version_id`); archive ở cấp Question.
2. Không có chức năng khôi phục ARCHIVED — endpoint `restore` cũ đã bỏ.
3. Xoá tài liệu = soft delete, bị chặn nếu `question_sources` còn tham chiếu.
4. Topic: chỉ đọc + tạo tối thiểu (API và import), không CRUD đầy đủ; Question không bắt buộc topic (`topic_id` nullable).
5. Rubric 1–1 với QuestionVersion; tổng điểm do GV nhập qua điểm từng tiêu chí, không cố định 10.
6. Phân bổ Bloom = số câu cụ thể mỗi mức, tổng = `totalQuestions`.
7. Giữ tên cột/enum hiện có: `question_content` = content, `reference_answer` = expected_answer, `generation_mode` (`AI_RAG`/`MANUAL`/`IMPORT`) = origin, `total_points`/`max_points` = total_score/max_score, `achievement_descriptors` = mô tả mức đạt, thêm `performance_levels` JSONB cho các mức chi tiết.
8. Giữ trạng thái `SUPERSEDED` cho bản APPROVED cũ khi có bản mới được duyệt (đã có trong repo).
9. Bloom do người nhập (MANUAL, IMPORT, bản CoW từ bản đã duyệt) coi là đã xác nhận; chỉ bản AI cần xác nhận.
10. Sinh lại một câu thay nội dung **ngay trên** bản DRAFT đó (index V6 chỉ cho phép một DRAFT mỗi câu); nội dung cũ lưu trong audit.
11. Sinh câu hỏi chạy đồng bộ trong request (≤ `max-questions-per-request` = 20); LLM/embedding gọi ngoài transaction.
12. Tài liệu READY không bao giờ quay về FAILED (chunk có thể đã được trích dẫn).

## 6. Thay đổi ngoài phạm vi nhóm 1 (bắt buộc)

- Khôi phục `LanguageLocale` + `LanguageLocaleConverter` (nhóm 7): commit `5726a47` xoá nhưng `SpeechConfigVersion*` vẫn dùng → `main` không compile.
- `GlobalExceptionHandler`: trả message cụ thể của `AppException` (trước đây luôn trả message mặc định của enum), thêm `error`/`errors`, map lỗi body/param sai và upload quá cỡ sang 4xx thay vì 500. Định dạng cũ (`code`, `message`) giữ nguyên.
- `SecurityConfig`: `/api/v1/lecturer/**` yêu cầu role LECTURER hoặc ADMIN.
- `@PreAuthorize` của controller giảng viên truyền thêm `authentication` (biểu thức cũ gọi hàm 1 tham số trong khi evaluator nhận 2 → lỗi runtime).

## 7. Hạn chế đã biết

- Đường OpenAI thật chỉ được test với `ChatModel` giả lập (không có API key trong CI); cần chạy thử với `VIEVA_AI_PROVIDER=openai` trước khi demo.
- Truy hồi dùng HNSW rồi lọc theo môn/tài liệu: khi dữ liệu nhiều môn rất lớn có thể trả ít hơn `top-k` (giới hạn `hnsw.ef_search`); có thể bật `hnsw.iterative_scan` (pgvector ≥ 0.8) nếu gặp.
- Kiểm trùng là Jaccard trên tập từ (ngưỡng 0.8), không phải ngữ nghĩa.
- API của nhóm 1 đổi path so với bản trước (`/questions/{id}/versions/{vid}/approve` → `/question-versions/{vid}/approve`, `generate-ai` → `question-generation-requests`, `manual` → `POST /subjects/{id}/questions`, `PATCH archive` → `POST archive`); FE hiện chưa gọi các API này.
