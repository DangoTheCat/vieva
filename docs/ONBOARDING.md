# Hướng dẫn chạy dự án AIVES (vieva) từ A tới Z

Tài liệu dành cho dev mới. Làm lần lượt từ trên xuống, mỗi bước có lệnh kiểm tra để chắc chắn bước đó đã chạy đúng.

---

## 0. Dự án gồm những gì

| Thành phần | Công nghệ | Thư mục | Cổng (port) |
| --- | --- | --- | --- |
| Database | PostgreSQL 16 + pgvector (Docker image `pgvector/pgvector:pg16`) | — | `5432` |
| Backend (BE) | Java 17, Spring Boot 3.4.3, LangChain4j, Maven | `BE/` | `8080` |
| Frontend (FE) | React 19, Vite 5, Tailwind CSS 3 | `FE/` | `5173` (dev) / `3000` (Docker) |

Ghi chú quan trọng:

- PostgreSQL khởi tạo database mới bằng `BE/src/main/resources/db/init.sh` và các SQL trong `db/migration`. Hibernate dùng `ddl-auto=none`; database hiện có không tự thay đổi.
- `V10__seed_initial_data.sql` tạo sẵn tài khoản, môn học, câu hỏi mẫu (xem mục 6).
- AI mặc định chạy ở chế độ **mock** (offline, không cần API key). Chỉ cần key thật khi muốn test với OpenAI.
- BE theo Clean Architecture: `domain/` → `application/` (use case, port) → `adapters/` (controller) → `infrastructure/` (DB, security, AI, storage).

---

## 1. Cài công cụ

| Công cụ | Phiên bản | Kiểm tra |
| --- | --- | --- |
| Git | bất kỳ | `git --version` |
| JDK | **17** (Temurin khuyến nghị) | `java -version` |
| Node.js | **20.x** trở lên (kèm npm 10) | `node -v` và `npm -v` |
| Docker Desktop | bản mới, bật WSL2 trên Windows | `docker version` và `docker compose version` |
| IDE | IntelliJ IDEA (BE), VS Code (FE) | — |
| (Tuỳ chọn) DBeaver / pgAdmin | xem dữ liệu DB | — |

Không cần cài Maven: repo có sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`).

Nếu máy đã cài sẵn PostgreSQL chạy dưới dạng service, hãy tắt service đó hoặc đổi port, vì nó sẽ chiếm cổng `5432` (xem mục 11).

---

## 2. Clone repo

```bash
git clone https://github.com/DangoTheCat/vieva.git
cd vieva
```

---

## 3. Tạo file `.env`

File `.env` nằm ở **thư mục gốc** của repo. File này bị `.gitignore` chặn, **không bao giờ commit**.

```bash
cp .env.example .env
```

Mở `.env` và điền tối thiểu hai biến:

```dotenv
DB_PASSWORD=root
JWT_SECRET=<chuỗi ngẫu nhiên, tối thiểu 32 ký tự>
```

Tạo `JWT_SECRET` bằng Git Bash:

```bash
openssl rand -base64 48
```

Gợi ý: đặt `DB_PASSWORD=root` ở máy local. Giá trị này trùng với giá trị mặc định trong `application.properties`, nên khi chạy BE từ IDE bạn không cần khai báo thêm biến môi trường cho DB. Chỉ dùng mật khẩu yếu này ở máy local.

Các biến còn lại (`OPENAI_API_KEY`, `CLOUDINARY_*`) có thể để trống khi dùng chế độ mock.

---

## 4. Cách A — Chạy toàn bộ bằng Docker Compose (nhanh nhất, để xem demo)

Chạy ở thư mục gốc:

```bash
docker compose up -d --build
```

Lần đầu mất vài phút (tải image, tải dependency Maven, `npm ci`).

Kiểm tra:

```bash
docker compose ps
docker compose logs -f backend
```

Đợi đến khi log backend có dòng `Started VievaApplication`. Sau đó mở:

- FE: http://localhost:3000 (Nginx tự proxy `/api/*` sang container backend)
- BE: http://localhost:8080

Dừng:

```bash
docker compose down
```

Cách này phù hợp để xem sản phẩm, **không** phù hợp để code hằng ngày vì mỗi lần sửa code phải build lại image. Để code, dùng cách B.

Docker Compose truyền `VIEVA_AI_PROVIDER` và cấu hình model từ `.env`; mặc định là `mock`. Chat và embedding thật chạy qua LangChain4j.

---

## 5. Cách B — Chế độ dev (khuyến nghị khi code)

DB chạy trong Docker, BE và FE chạy trực tiếp trên máy để có hot reload và debug được.

### 5.1. Bật database

```bash
docker compose up -d postgres
docker compose ps
```

Cột `STATUS` của `vieva-postgres` phải là `healthy`.

### 5.2. Chạy Backend

**Lưu ý:** Spring Boot **không tự đọc file `.env`**. Nếu `.env` có giá trị khác mặc định (ví dụ `DB_PASSWORD` khác `root`), bạn phải nạp biến vào shell hoặc vào Run Configuration của IDE trước khi chạy.

**PowerShell (Windows):**

```powershell
cd BE
Get-Content ..\.env | Where-Object { $_ -match '^\s*[^#\s].*=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$($k.Trim())" $v.Trim() }
.\mvnw.cmd spring-boot:run
```

**Git Bash / macOS / Linux:**

```bash
cd BE
set -a; source ../.env; set +a
./mvnw spring-boot:run
```

LangChain4j nhận `OPENAI_BASE_URL` có sẵn phiên bản API, ví dụ `https://api.openai.com/v1` hoặc `https://generativelanguage.googleapis.com/v1beta/openai`. Không dùng `OPENAI_CHAT_PATH` và `OPENAI_EMBEDDING_PATH` nữa.

**IntelliJ IDEA:**

1. Mở thư mục `BE/` dưới dạng Maven project.
2. Chọn Project SDK là JDK 17. Bật *Annotation Processing* (cho Lombok).
3. Chạy class `com.example.vieva.VievaApplication`.
4. Nếu cần, thêm biến môi trường trong *Run Configuration → Environment variables* (ví dụ `DB_PASSWORD=...;JWT_SECRET=...`). Có thể dùng plugin EnvFile để nạp thẳng `.env`.

Khi tạo volume PostgreSQL mới, init script chạy toàn bộ SQL theo thứ tự version và seed dữ liệu mẫu. Backend khởi động thành công khi log có `Started VievaApplication`.

Kiểm tra nhanh bằng API đăng nhập (Git Bash):

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"admin@aives.edu.vn","password":"password123"}'
```

Kết quả trả về chứa JWT token nghĩa là BE và DB đã hoạt động.

### 5.3. Chạy Frontend

Mở terminal thứ hai:

```bash
cd FE
npm install
npm run dev
```

Mở http://localhost:5173.

Vite proxy mọi request `/api/*` sang `http://localhost:8080` (cấu hình trong `FE/vite.config.js`), nên **không cần** cấu hình CORS hay URL backend. Nếu muốn FE gọi một backend khác, đặt biến `VITE_API_BASE_URL` trong file `FE/.env.local`.

Lưu ý: một số màn hình FE có chế độ dữ liệu giả (mock fallback) khi không gọi được backend. Nếu bạn thấy dữ liệu "lạ" không có trong DB, hãy kiểm tra xem BE có đang chạy ở cổng 8080 không.

---

## 6. Tài khoản có sẵn (seed từ `V10`)

Tất cả dùng mật khẩu `password123`.

| Vai trò | Email | Ghi chú |
| --- | --- | --- |
| Admin | `admin@aives.edu.vn` | Quản lý người dùng, môn học |
| Giảng viên | `vancee@fpt.edu.vn` | Phụ trách SWD392, PRN231 |
| Giảng viên | `vancem@fpt.edu.vn` | Phụ trách SWD392, PRN211 |
| Sinh viên | `student@fpt.edu.vn` | Mã SE170001 |

Đây là dữ liệu dev, không dùng cho môi trường thật.

---

## 7. Bật AI thật và lưu file lên Cloudinary (tuỳ chọn)

Mặc định mọi thứ chạy offline. Để bật dịch vụ thật, đặt thêm biến môi trường trước khi chạy BE:

| Mục đích | Biến môi trường |
| --- | --- |
| Sinh câu hỏi / embedding bằng OpenAI | `VIEVA_AI_PROVIDER=openai`, `OPENAI_BASE_URL=https://api.openai.com/v1`, `OPENAI_API_KEY=sk-...` (tuỳ chọn `OPENAI_CHAT_MODEL`, `OPENAI_EMBEDDING_MODEL`) |
| Dùng Gemini thay OpenAI (endpoint OpenAI-compatible) | `VIEVA_AI_PROVIDER=gemini`, `OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai`, `OPENAI_CHAT_MODEL=gemini-3.5-flash-lite`, `OPENAI_EMBEDDING_MODEL=gemini-embedding-001`, `OPENAI_API_KEY=<Gemini key>` |
| Lưu tài liệu lên Cloudinary | `VIEVA_STORAGE_PROVIDER=cloudinary`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` |
| Thư mục lưu file local | `VIEVA_STORAGE_ROOT` (mặc định `./data/documents`) |
| Gửi email thật khi admin tạo tài khoản | `VIEVA_MAIL_PROVIDER=smtp`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail: App Password), `MAIL_FROM`, `APP_LOGIN_URL`. Mặc định `log`: chỉ ghi log người nhận và tiêu đề, không gửi |

Không bao giờ commit API key. Mỗi người tự xin key và để trong `.env` của mình.

RAG lấy tối đa `2 × vieva.rag.top-k` ứng viên bằng vector trong các tài liệu READY đã chọn của môn học, rồi xếp lại theo 75% cosine similarity + 25% Jaccard từ khóa. Đây là reranking trên ứng viên vector; chưa có nhánh tìm kiếm full-text độc lập. Giữ nguyên model embedding và số chiều khi dùng lại vector đã lưu; đổi model cần index lại tài liệu.

Các endpoint tốn chi phí AI (chat, generate, retry, import, upload) bị giới hạn 30 request/phút mỗi IP. Endpoint auth bị giới hạn 10 request/phút.

---

## 8. Chạy test

**Backend** (cần Docker Desktop đang chạy, vì `QuestionBankIntegrationTest` dùng Testcontainers để dựng PostgreSQL + pgvector thật):

```bash
cd BE
./mvnw test
```

Trên PowerShell dùng `.\mvnw.cmd test`. Chạy một class:

```bash
./mvnw test -Dtest=UserControllerTest
```

Lưu ý: script chạy test qua Docker (`run-tests.ps1`, `run-tests.bat` hoặc service `test` trong `docker-compose.yml`) bỏ qua test tích hợp; muốn chạy đủ thì dùng `BE/mvnw test` trên máy có Docker Desktop.

**Frontend**: hiện chưa có test tự động. Kiểm tra bằng build:

```bash
cd FE
npm run build
```

---

## 9. Kiến trúc backend (Clean Architecture)

Backend được tổ chức theo chuẩn Clean Architecture gồm 4 tầng độc lập, tuân thủ nghiêm ngặt Dependency Rule: phụ thuộc mã nguồn chỉ trỏ từ ngoài vào trong.

### 9.1. Bảng phụ thuộc 4 tầng

| Từ \ Đến | domain | application | adapters | infrastructure |
|---|---|---|---|---|
| domain | được | **cấm** | **cấm** | **cấm** |
| application | được | được | **cấm** | **cấm** |
| adapters | **cấm** | chỉ `ports/input`, `exception` | được | **cấm** |
| infrastructure | chỉ gateway `database` map dữ liệu | chỉ `ports/*`, `exception`, `settings` | **cấm** | được |
| configuration (Main) | được | được | được | được |

### 9.2. Luồng request chuẩn (không nhảy bậc)

```
HTTP JSON → Controller → Request Model → Input Port (interface) → Interactor → Entity
                                                                     │
                                                                     ├─▶ Output Port (interface) ◀── Gateway → JPA/DB
                                                                     ▼
HTTP JSON ← Presenter ← Response Model ◀─────────────────────────────┘
```

- **Controller**: Nhận HTTP request, bind vào Request Model (record/POJO thuần Java), gọi Input Port. Cấm gọi repository hoặc use case cụ thể.
- **Input Port**: Interface hợp đồng nghiệp vụ, không rò rỉ kiểu hay entity của domain.
- **Interactor (Use Case)**: Chứa nghiệp vụ ứng dụng, điều phối domain entity và giao tiếp ngoại vi qua Output Port.
- **Output Port**: Interface do application làm chủ (Repository, Gateway AI, Storage, Event Publisher).
- **Gateway (Infrastructure)**: Implement Output Port, map qua lại giữa JPA entity/external API và domain model.
- **Presenter**: Map Response Model (View/Result) sang HTTP JSON cho client.

### 9.3. Bảng quy tắc kiến trúc (R1–R9)

Bảng dưới đây là các quy tắc kiến trúc mục tiêu để review. Hiện repository chưa có `ArchitectureTest.java` hoặc dependency ArchUnit; không coi bảng này là các kiểm tra tự động đã được thực thi.

| Luật | Tên luật ArchUnit | Ý nghĩa tóm tắt |
|---|---|---|
| **R1** | `domainIsPure` | Domain hoàn toàn thuần khiết, cấm phụ thuộc vào tầng ngoài hay framework (Spring, JPA). |
| **R2** | `applicationIsFrameworkFree` | Application chỉ chứa use case và port thuần Java, cấm phụ thuộc Spring, JPA, adapters hay infrastructure. |
| **R3** | `adaptersUseInputPortsOnly` | Adapters (controller, presenter) chỉ giao tiếp qua Input Port, cấm nhảy bậc sang domain, infra hoặc output port. |
| **R4** | `infraDoesNotCallInteractors` | Infrastructure (filter, worker, scheduler) cấm gọi trực tiếp implementation use case cụ thể hoặc adapters. |
| **R5** | `onlyDbGatewaysTouchDomain` | Ngoài gateway database (map dữ liệu) và configuration (Main), cấm mọi class trong infrastructure chạm domain. |
| **R6** | `entitiesHaveNoSetters` | Toàn bộ domain không có setter public; thay đổi trạng thái bắt buộc qua business behavior methods. |
| **R7** | `adaptersDoNotDependOnUseCases` | Adapters cấm phụ thuộc vào implementation cụ thể của use case (`application.usecases..`). |
| **R8** | `adaptersDoNotDependOnOutputRepositoriesOrPorts` | Adapters cấm phụ thuộc vào output repository hoặc output port (`application.ports.output..`). |
| **R9** | `inputPortsDoNotDependOnDomain` | Input ports (request/response/command/query/view) hoàn toàn độc lập, không mang kiểu hay entity của domain. |

### 9.4. Ranh giới Aggregate (ports/output) và View (ports/input)

- **Aggregate (`ports/output/`)**: Gom nhóm các domain entities (ví dụ `QuestionVersionAggregate` gom `Question`, `QuestionVersion`, `Topic`, `Rubric`, `RubricCriterion`, `QuestionSource`), chỉ lưu hành nội bộ giữa `application` và `infrastructure/database` (repository, gateway, database assembler). Tuyệt đối cấm `adapters` chạm tới Aggregate.
- **View / Result (`ports/input/`)**: Response model thuần Java / record (ví dụ `QuestionVersionView`, `UserResult`, `SubjectResult`), độc lập hoàn toàn với `domain`. Use case sử dụng mapper nội bộ (`*ViewMapper`) để chuyển đổi từ Aggregate / Entity sang View / Result trước khi trả ra cho `adapters` (controller, presenter).

### 9.5. Nguyên tắc kiểm thử (Testing Principles)

1. **JSON Snapshot Tests**: Mọi thay đổi về cấu trúc DTO / contract phải có snapshot tests bảo vệ (`JsonContractSnapshotTest`, `AiPromptAndGatewaySnapshotTest`), đảm bảo JSON payload và HTTP status code không bao giờ bị phá vỡ ngoài ý muốn.
2. **Không nhét logic nghiệp vụ vào mock (`No business logic in mocks`)**: Tuyệt đối không dùng `lenient().when(...).thenAnswer(...)` trong mock để tự parse enum, validate tham số hay tái hiện logic nghiệp vụ. Muốn kiểm tra logic thì phải dùng use case / service thật với các output port được mock.
3. **Luôn chạy `mvnw clean test`**: Trước khi commit và tạo PR, bắt buộc chạy `mvnw clean test` để kiểm tra toàn bộ unit tests, contract snapshots và integration tests hiện có. Quy tắc kiến trúc ở mục 9.3 được review riêng, chưa có bộ ArchUnit tự động.

---

## 10. Quy tắc làm việc

### 10.1. Database migration

- Muốn đổi schema: tạo file mới `BE/src/main/resources/db/migration/V15__mo_ta_ngan.sql` (số version tiếp theo).
- SQL đã áp dụng trên database hiện có không tự chạy lại. Thay đổi schema phải áp dụng thủ công bằng script mới, sau khi sao lưu.
- Khi sửa entity JPA, cập nhật SQL khởi tạo và áp dụng SQL tương ứng lên database hiện có; Hibernate dùng `ddl-auto=none` nên không tự thêm cột.

### 10.2. Studio và quy trình duyệt

- Backend studio: `/api/v1/lecturer/studio`. FE hiện tại chưa tích hợp nút gửi duyệt hay chuông.
- Upload: `POST /subjects/{subjectId}/documents` (multipart `file`) trả `202 UPLOADED`; worker chuyển `INDEXING → READY`, hoặc `FAILED` nếu lỗi. Poll endpoint document hiện có. `POST /documents/{documentId}/archive` ngừng dùng tài liệu trong retrieval và giữ nguyên nguồn lịch sử.
- Sinh một câu: `POST /questions/generate` với `{ "topicId": "<uuid>", "difficulty": "APPLY", "documentIds": ["<uuid>"] }`. Bỏ `documentIds` để dùng tài liệu READY của môn; `difficulty` là một trong sáu mã Bloom. Trả `201` với câu hỏi, đáp án và `rubricJson`, tổng điểm 10.0. Batch API cũ tiếp tục hoạt động.
- Chỉ ADMIN có quyền tạo môn học (`POST /api/v1/admin/subjects`) và phân công/thu hồi giảng viên (`POST|DELETE /api/v1/admin/lecturer-subjects`). Vai trò trưởng bộ môn (head) đã được bãi bỏ hoàn toàn khỏi backend API và UI; giảng viên chỉ thao tác trong phạm vi các môn được admin phân công.
- Tác giả sửa/xác nhận Bloom rồi `POST /questions/{questionId}/submit-for-review` với `{ "versionId": "<uuid>", "expectedVersion": 0 }`. Lấy lockVersion mới từ response cho bước tiếp theo.
- Quy trình duyệt câu hỏi: Tác giả (người tạo) trực tiếp xem xét và phê duyệt câu hỏi của mình qua `GET /questions/{questionId}/review?versionId=<uuid>`, sau đó `POST /questions/{questionId}/approve` hoặc `/reject` (thêm `reason`). Chỉ trạng thái UNDER_REVIEW mới được approve/reject. Người không phải tác giả sẽ nhận lỗi `NOT_QUESTION_AUTHOR`. Tác giả có thể sửa câu hỏi bị REJECTED rồi submit lại; trạng thái UNDER_REVIEW bị khóa chỉnh sửa nội dung.
- Thông báo trong DB: `GET /api/v1/notifications?page=0&size=20`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read`. Chỉ người nhận được đọc/đánh dấu; thay đổi trạng thái và thông báo commit cùng nhau.
- Áp dụng `V16__studio_review_and_notifications.sql` thủ công cho database cũ sau khi sao lưu. Không bật Flyway và không reset volume. SQL bảo toàn version/rubric/source, thêm trạng thái và notification. Schema JSON gửi model dùng evidence IDs; quote lưu trong DB luôn lấy nguyên văn từ evidence được chọn.

### 10.3. Git

- Không push thẳng vào `main`. Tạo nhánh từ `main` mới nhất:

  ```bash
  git checkout main
  git pull
  git checkout -b feat/ten-tinh-nang
  ```

- Đặt tên nhánh theo mẫu đang dùng: `feat/...`, `fix/...`.
- Commit theo Conventional Commits: `feat: ...`, `fix: ...`, `fix(review): ...`.
- Mở Pull Request vào `main` trên GitHub để review rồi mới merge.

---

## 11. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `docker compose up` báo `DB_PASSWORD env var is required` hoặc `JWT_SECRET env var is required` | Chưa có `.env` ở thư mục gốc, hoặc biến còn trống | Làm lại mục 3 |
| `port 5432 ... already allocated` | Máy đang chạy PostgreSQL cài sẵn | Tắt service PostgreSQL local (Windows: `services.msc`), hoặc đổi mapping trong `docker-compose.yml` thành `127.0.0.1:5433:5432` và đặt `DB_PORT=5433` khi chạy BE |
| BE báo `password authentication failed for user "postgres"` | Mật khẩu BE dùng khác mật khẩu lúc tạo volume DB, hoặc chưa nạp `.env` vào shell | Nạp `.env` như mục 5.2. Nếu đã đổi `DB_PASSWORD` sau lần chạy đầu, xem dòng reset DB bên dưới |
| BE báo `Schema-validation: missing table/column` | Entity khác với schema | Viết migration mới (mục 10.1) |
| Cần reset DB về trạng thái sạch | — | Xem cảnh báo bên dưới bảng |
| FE hiện dữ liệu giả / gọi API lỗi `502`, `ECONNREFUSED` | BE chưa chạy hoặc chạy sai cổng | Chạy BE ở cổng 8080, kiểm tra bằng lệnh `curl` ở mục 5.2 |
| `429 Too Many Requests` | Vượt rate limit | Đợi 1 phút |
| Test BE lỗi `Could not find a valid Docker environment` | Docker Desktop chưa bật | Bật Docker Desktop rồi chạy lại |
| `./mvnw: Permission denied` (Git Bash) | Mất quyền thực thi | `chmod +x mvnw`, hoặc dùng `mvnw.cmd` |

**Reset DB local — cảnh báo:** lệnh dưới đây **xoá vĩnh viễn toàn bộ dữ liệu** trong volume `postgres_data`. Lần tạo container PostgreSQL với volume mới, init script tạo schema và seed từ đầu.

```bash
docker compose down -v
docker compose up -d postgres
```

---

## 12. Checklist ngày đầu

- [ ] Cài JDK 17, Node 20, Docker Desktop, Git
- [ ] Clone repo, tạo `.env` có `DB_PASSWORD` và `JWT_SECRET`
- [ ] `docker compose up -d postgres` → trạng thái `healthy`
- [ ] Chạy BE → log có `Started VievaApplication`
- [ ] `curl` đăng nhập admin trả về token
- [ ] `npm install` và `npm run dev` trong `FE/` → đăng nhập được ở http://localhost:5173
- [ ] `./mvnw test` pass
- [ ] Tạo nhánh `feat/...` đầu tiên

