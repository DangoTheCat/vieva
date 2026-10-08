# Hướng dẫn chạy dự án AIVES (vieva) từ A tới Z

Tài liệu dành cho dev mới. Làm lần lượt từ trên xuống, mỗi bước có lệnh kiểm tra để chắc chắn bước đó đã chạy đúng.

---

## 0. Dự án gồm những gì

| Thành phần | Công nghệ | Thư mục | Cổng (port) |
| --- | --- | --- | --- |
| Database | PostgreSQL 16 + pgvector (Docker image `pgvector/pgvector:pg16`) | — | `5432` |
| Backend (BE) | Java 17, Spring Boot 3.4.3, Spring AI 1.0, Flyway, Maven | `BE/` | `8080` |
| Frontend (FE) | React 19, Vite 5, Tailwind CSS 3 | `FE/` | `5173` (dev) / `3000` (Docker) |

Ghi chú quan trọng:

- **Flyway quản lý toàn bộ schema DB** (`BE/src/main/resources/db/migration`, từ `V0` đến `V11`). Hibernate chạy ở chế độ `ddl-auto=validate`, nghĩa là nó **không** tự tạo hoặc sửa bảng.
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

Nếu máy đã cài sẵn PostgreSQL chạy dưới dạng service, hãy tắt service đó hoặc đổi port, vì nó sẽ chiếm cổng `5432` (xem mục 10).

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

Lưu ý: file `docker-compose.yml` không truyền `VIEVA_AI_PROVIDER`, nên backend trong Docker luôn chạy AI ở chế độ mock.

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

**Cảnh báo cho Git Bash trên Windows:** Git Bash tự đổi giá trị biến môi trường bắt đầu bằng `/` thành đường dẫn Windows. Ví dụ `OPENAI_EMBEDDING_PATH=/embeddings` bị đổi thành `C:/Program Files/Git/embeddings`, và BE báo lỗi `invalid URI scheme c` khi gọi AI. Trên Windows, hãy chạy BE bằng PowerShell hoặc IntelliJ.

**IntelliJ IDEA:**

1. Mở thư mục `BE/` dưới dạng Maven project.
2. Chọn Project SDK là JDK 17. Bật *Annotation Processing* (cho Lombok).
3. Chạy class `com.example.vieva.VievaApplication`.
4. Nếu cần, thêm biến môi trường trong *Run Configuration → Environment variables* (ví dụ `DB_PASSWORD=...;JWT_SECRET=...`). Có thể dùng plugin EnvFile để nạp thẳng `.env`.

Khi khởi động lần đầu, Flyway chạy toàn bộ migration `V0` → `V11` và seed dữ liệu mẫu. Log thành công có dòng `Started VievaApplication`.

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
| Sinh câu hỏi / embedding bằng OpenAI | `VIEVA_AI_PROVIDER=openai`, `OPENAI_API_KEY=sk-...` (tuỳ chọn `OPENAI_CHAT_MODEL`, `OPENAI_EMBEDDING_MODEL`) |
| Dùng Gemini thay OpenAI (endpoint OpenAI-compatible) | `OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai`, `OPENAI_CHAT_PATH=/chat/completions`, `OPENAI_EMBEDDING_PATH=/embeddings`, `OPENAI_CHAT_MODEL=gemini-3.5-flash-lite`, `OPENAI_EMBEDDING_MODEL=gemini-embedding-001`, `OPENAI_API_KEY=<Gemini key>` |
| Lưu tài liệu lên Cloudinary | `VIEVA_STORAGE_PROVIDER=cloudinary`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` |
| Thư mục lưu file local | `VIEVA_STORAGE_ROOT` (mặc định `./data/documents`) |
| Gửi email thật khi admin tạo tài khoản | `VIEVA_MAIL_PROVIDER=smtp`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail: App Password), `MAIL_FROM`, `APP_LOGIN_URL`. Mặc định `log`: chỉ ghi log người nhận và tiêu đề, không gửi |

Không bao giờ commit API key. Mỗi người tự xin key và để trong `.env` của mình.

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

**Frontend**: hiện chưa có test tự động. Kiểm tra bằng build:

```bash
cd FE
npm run build
```

---

## 9. Quy tắc làm việc

### 9.1. Database migration

- Muốn đổi schema: tạo file mới `BE/src/main/resources/db/migration/V12__mo_ta_ngan.sql` (số version tiếp theo).
- **Không sửa** migration đã merge vào `main`. Flyway lưu checksum, sửa file cũ sẽ làm máy của cả nhóm báo lỗi `checksum mismatch`.
- Sửa entity JPA thì phải có migration tương ứng, nếu không BE sẽ không khởi động được (do `ddl-auto=validate`).

### 9.2. Git

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

## 10. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `docker compose up` báo `DB_PASSWORD env var is required` hoặc `JWT_SECRET env var is required` | Chưa có `.env` ở thư mục gốc, hoặc biến còn trống | Làm lại mục 3 |
| `port 5432 ... already allocated` | Máy đang chạy PostgreSQL cài sẵn | Tắt service PostgreSQL local (Windows: `services.msc`), hoặc đổi mapping trong `docker-compose.yml` thành `127.0.0.1:5433:5432` và đặt `DB_PORT=5433` khi chạy BE |
| BE báo `password authentication failed for user "postgres"` | Mật khẩu BE dùng khác mật khẩu lúc tạo volume DB, hoặc chưa nạp `.env` vào shell | Nạp `.env` như mục 5.2. Nếu đã đổi `DB_PASSWORD` sau lần chạy đầu, xem dòng reset DB bên dưới |
| BE báo `Schema-validation: missing table/column` | Entity khác với schema | Viết migration mới (mục 9.1) |
| BE báo Flyway `checksum mismatch` hoặc `Validate failed` | Có migration cũ bị sửa | Báo nhóm. Ở máy local, có thể reset DB (dòng bên dưới) |
| Cần reset DB về trạng thái sạch | — | Xem cảnh báo bên dưới bảng |
| FE hiện dữ liệu giả / gọi API lỗi `502`, `ECONNREFUSED` | BE chưa chạy hoặc chạy sai cổng | Chạy BE ở cổng 8080, kiểm tra bằng lệnh `curl` ở mục 5.2 |
| `429 Too Many Requests` | Vượt rate limit | Đợi 1 phút |
| Test BE lỗi `Could not find a valid Docker environment` | Docker Desktop chưa bật | Bật Docker Desktop rồi chạy lại |
| `./mvnw: Permission denied` (Git Bash) | Mất quyền thực thi | `chmod +x mvnw`, hoặc dùng `mvnw.cmd` |

**Reset DB local — cảnh báo:** lệnh dưới đây **xoá vĩnh viễn toàn bộ dữ liệu** trong volume `postgres_data`. Lần chạy BE tiếp theo, Flyway tạo lại schema và seed từ đầu.

```bash
docker compose down -v
docker compose up -d postgres
```

---

## 11. Checklist ngày đầu

- [ ] Cài JDK 17, Node 20, Docker Desktop, Git
- [ ] Clone repo, tạo `.env` có `DB_PASSWORD` và `JWT_SECRET`
- [ ] `docker compose up -d postgres` → trạng thái `healthy`
- [ ] Chạy BE → log có `Started VievaApplication`
- [ ] `curl` đăng nhập admin trả về token
- [ ] `npm install` và `npm run dev` trong `FE/` → đăng nhập được ở http://localhost:5173
- [ ] `./mvnw test` pass
- [ ] Tạo nhánh `feat/...` đầu tiên
