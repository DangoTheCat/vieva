# 🎙️ AIVES - Smart Oral Exam System
> **Hệ thống Thi Vấn đáp Thông minh Ứng dụng Trí tuệ Nhân tạo**  
> *Môn học: SWD392 — Trình bày Báo cáo Milestone 2*

[![Course](https://img.shields.io/badge/Course-SWD392-blue.svg?style=for-the-badge)](https://fpt.edu.vn)
[![Milestone](https://img.shields.io/badge/Milestone-2%20APPROVED-success.svg?style=for-the-badge)]()
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-orange.svg?style=for-the-badge)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)]()

---

## 📑 Mục Lục
1. [Tổng Quan Dự Án](#-tổng-quan-dự-án)
2. [Đặt Vấn Đề & Giải Pháp](#-đặt-vấn-đề--giải-pháp)
3. [Các Vai Trò Người Dùng (User Roles)](#-các-vai-trò-người-dùng-user-roles)
4. [3 Quy Trình Nghiệp Vụ Cốt Lõi (Core Workflows)](#-3-quy-trình-nghiệp-vụ-cốt-lõi-core-workflows)
5. [Danh Mục Use Cases Chi Tiết](#-danh-mục-use-cases-chi-tiết)
6. [Kiến Trúc Phần Mềm & Công Nghệ](#-kiến-trúc-phần-mềm--công-nghệ)
7. [Mô Hình Dữ Liệu Khái Niệm (ERD)](#-mô-hình-dữ-liệu-khái-niệm-erd)
8. [Cấu Trúc Thư Mục Repository](#-cấu-trúc-thư-mục-repository)
9. [Hướng Dẫn Cài Đặt & Khởi Chạy Local](#-hướng-dẫn-cài-đặt--khởi-chạy-local)
10. [Cấu Hình Biến Môi Trường (.env)](#-cấu-hình-biến-môi-trường-env)
11. [Danh Sách API & WebSocket Endpoints](#-danh-sách-api--websocket-endpoints)
12. [Đóng Góp & Lịch Trình Dự Án (Roadmap)](#-đóng-góp--lịch-trình-dự-án-roadmap)

---

## 🎯 Tổng Quan Dự Án

**AIVES (Artificial Intelligence Voice Evaluation System)** là hệ thống tổ chức thi vấn đáp trực tuyến tự động dành cho các cơ sở giáo dục đại học. Hệ thống giải quyết triệt để bài toán tốn kém nhân lực chấm thi, thiếu tính đồng nhất trong đánh giá và nguy cơ lộ đề thi vấn đáp truyền thống.

AIVES kết hợp các công nghệ tiên tiến nhất:
* **RAG (Retrieval-Augmented Generation):** Trích xuất tri thức chính xác từ slide/giáo trình môn học, chống hiện tượng "ảo giác" (hallucination) của AI.
* **Real-time Audio Streaming (STT & TTS):** Cho phép tương tác giọng nói trực tiếp hai chiều với độ trễ thấp giữa Sinh viên và Giám thị AI.
* **Adaptive Follow-up Question Engine (LLM):** Khả năng "hỏi xoáy" thích ứng dựa trên câu trả lời thực tế của sinh viên và Rubric chuẩn hóa.

---

## 💡 Đặt Vấn Đề & Giải Pháp

| Thách thức truyền thống | Giải pháp của AIVES |
| :--- | :--- |
| **Giảng viên tốn nhiều giờ làm giám thị:** Mỗi buổi thi vấn đáp kéo dài nhiều ngày. | **Hệ thống phỏng vấn tự động:** Sinh viên thi trực tiếp với AI qua giao diện Web audio streaming. |
| **Đánh giá cảm tính:** Điểm số phụ thuộc vào tâm lý/sự khắt khe của từng giám thị. | **Rubric chuẩn hóa & Snapshot đề thi:** Đánh giá tự động dựa trên đáp án tham chiếu và khung điểm cố định. |
| **Ngân hàng câu hỏi nghèo nàn:** Dễ bị trùng lặp câu hỏi giữa các ca thi. | **Sinh câu hỏi bằng RAG:** AI sinh câu hỏi, đáp án và trích dẫn trực tiếp từ giáo trình được duyệt. |
| **Lộ đề thi giữa các ca:** Sinh viên thi trước chia sẻ câu hỏi cho sinh viên thi sau. | **Hỏi xoáy thích ứng (Adaptive Follow-up):** Đề thi linh hoạt thay đổi theo khả năng phản biện của từng cá nhân. |

---

## 👥 Các Vai Trò Người Dùng (User Roles)

* 👨‍🏫 **Giảng viên (Lecturer):** Tải tài liệu môn học, cấu hình tham số RAG, duyệt/biên tập câu hỏi từ `DRAFT` sang `APPROVED`, quản lý ngân hàng câu hỏi và xem kết quả chấm thi.
* 🎓 **Sinh viên (Student):** Kiểm tra thiết bị mic/loa, tham gia lượt thi vấn đáp giọng nói thời gian thực, lắng nghe câu hỏi TTS, trả lời bằng giọng nói (STT) và hoàn tất bài thi.
* 🛠️ **Quản trị viên (Administrator):** Quản lý tài khoản, gán vai trò, phân công giảng viên phụ trách môn học, cấu hình ngôn ngữ (vi-VN/en-US) và thử nghiệm chất lượng giọng đọc STT/TTS.

---

## 🔄 3 Quy Trình Nghiệp Vụ Cốt Lõi (Core Workflows)

### 🔵 WF01 — Ngân hàng Câu hỏi & RAG (Nhóm 1)
1. **Khởi tạo:** Giảng viên chọn môn học, tải tệp giáo trình/slide (PDF, DOCX) và thiết lập tham số sinh câu hỏi (số lượng, ma trận cấp độ tư duy Bloom).
2. **Xử lý AI:** Backend AIVES trích xuất văn bản, cắt chunk, gọi Dịch vụ Embedding để tạo vector. Dịch vụ LLM nhận ngữ cảnh và sinh bản nháp câu hỏi, đáp án, Rubric và nguồn trích dẫn (`Citation`).
3. **Phê duyệt:** AIVES lưu bản ghi `DRAFT`. Giảng viên kiểm tra, chỉnh sửa và bấm Duyệt để chuyển thành phiên bản `APPROVED` chính thức.

### 🟢 WF03 — Phỏng vấn AI & Hỏi Xoáy Thích Ứng (Nhóm 3)
1. **Khởi tạo:** Sinh viên kiểm tra thiết bị. AIVES tạo lượt thi (`ExamAttempt`), chốt **Snapshot bất biến** đề thi và Rubric.
2. **Tương tác Thoại:** AIVES gửi câu hỏi sang Dịch vụ **TTS** phát âm thanh. Sinh viên trả lời bằng giọng nói; luồng audio được stream qua WebSocket sang Dịch vụ **STT** để hiển thị `transcript partial` và chốt `transcript final`.
3. **Đánh giá Thích ứng:** LLM phân tích câu trả lời đối chiếu với Rubric. Nếu câu trả lời chưa rõ/thiếu ý và còn thời gian/ngân sách lượt hỏi, LLM tạo câu hỏi phụ (`FOLLOW_UP`) và lặp lại bước phát TTS.
4. **Đóng phiên:** Khi hết câu hỏi hoặc hết giờ, AIVES lưu trữ bền vững toàn bộ băng ghi âm, transcript và bàn giao dữ liệu chấm điểm.

### 🟣 WF07 — Quản trị Hệ thống & Cấu hình Giọng nói (Nhóm 7)
1. **Quản lý Tài khoản & Phân công:** Admin tạo tài khoản, phân gán vai trò (`LECTURER`, `STUDENT`) và liên kết Giảng viên với Môn học phụ trách (`LecturerSubject`).
2. **Cấu hình & Thử nghiệm Giọng nói:** Admin thiết lập ngôn ngữ mặc định (`vi-VN` / `en-US`), chọn giọng đọc TTS. AIVES gửi mẫu thử nghiệm sang STT/TTS. Chỉ khi thử nghiệm thành công, phiên bản cấu hình giọng nói mới (`SpeechConfigVersion`) mới được kích hoạt nguyên tử.

---

## 📋 Danh Mục Use Cases Chi Tiết

<details open>
<summary><b>Mở rộng / Thu gọn Danh mục Use Cases</b></summary>

| Nhóm | Mã UC | Tên Use Case | Actor Chính | Mô Tả Ngắn |
| :--- | :--- | :--- | :--- | :--- |
| **Group 1: Ngân Hàng** | `UC1.1` | Tải & chỉ mục tài liệu (RAG) | Giảng viên | Upload tài liệu môn học, cắt chunk và tạo vector index |
| | `UC1.2` | Sinh câu hỏi & Rubric bằng RAG | Giảng viên, AI | LLM sinh câu hỏi, đáp án, nguồn trích dẫn dựa trên tài liệu |
| | `UC1.3` | Duyệt & biên tập câu hỏi | Giảng viên | Rà soát bản nháp, chỉnh sửa và phê duyệt `DRAFT -> APPROVED` |
| | `UC1.4` | Tra cứu & lưu trữ ngân hàng | Giảng viên | Tìm kiếm câu hỏi theo môn/Bloom, chuyển trạng thái `ARCHIVED` |
| | `UC1.5` | Tạo câu hỏi thủ công | Giảng viên | Tự nhập câu hỏi, đáp án và Rubric không qua AI |
| | `UC1.6` | Nhập câu hỏi từ tệp | Giảng viên | Import danh sách câu hỏi hàng loạt từ file Excel/CSV |
| | `UC1.7` | Quản lý Rubric chi tiết | Giảng viên | Thêm/sửa tiêu chí đánh giá (`RubricCriterion`) và khung điểm |
| **Group 3: Phỏng Vấn AI** | `UC3.1` | Khởi tạo & kiểm tra thiết bị | Sinh viên | Check mic/loa, nạp snapshot bài thi và tạo `ExamAttempt` |
| | `UC3.2` | Phát câu hỏi bằng TTS | System, TTS | Chuyển văn bản câu hỏi chính/phụ thành giọng nói phát cho SV |
| | `UC3.3` | Thu âm & chuyển thoại STT | Sinh viên, STT | Record voice sinh viên, stream WebSocket để nhận diện transcript |
| | `UC3.4` | Hỏi xoáy thích ứng (LLM) | System, LLM | Phân tích câu trả lời, sinh câu hỏi phụ nếu trả lời chưa đạt |
| | `UC3.5` | Hoàn tất & lưu lượt thi | Sinh viên | Dừng thu âm, chốt phiên thi, lưu bền vững bằng chứng âm thanh |
| **Group 7: Quản Trị** | `UC7.1` | Quản lý tài khoản & vai trò | Admin | Tạo, sửa, khóa tài khoản và phân vai trò (`ADMIN/LECTURER/STUDENT`) |
| | `UC7.2` | Cấu hình STT/TTS locale | Admin, STT/TTS| Cấu hình giọng nói tiếng Việt/Anh, thử nghiệm trước khi kích hoạt |
| | `UC7.4` | Phân công Giảng viên - Môn | Admin | Gán quyền phụ trách môn học cụ thể cho từng Giảng viên |

</details>

---

## 🏗️ Kiến Trúc Phần Mềm & Công Nghệ

Hệ thống được thiết kế theo mô hình **Modular Monolith** nhằm tối ưu chi phí vận hành, đảm bảo tính toàn vẹn giao dịch (ACID) nhưng vẫn phân tách ranh giới các module độc lập.

```text
+-----------------------------------------------------------------------+
|                           WEB CLIENT APP                              |
|                   (React.js / Next.js SPA / Tailwind)                 |
+-----------------------------------+-----------------------------------+
                                    |
                    REST API (HTTP) | WebSocket (Real-time Audio Stream)
                                    |
+-----------------------------------v-----------------------------------+
|                        AIVES BACKEND CORE                             |
|                     (Modular Monolith Framework)                      |
|                                                                       |
|  +---------------------+  +--------------------+  +----------------+  |
|  | QuestionBank Module |  | Interview Module   |  | Admin Module   |  |
|  | (RAG, Question,     |  | (State Machine,    |  | (User, Auth,   |  |
|  |  Rubric, Document)  |  |  WebSocket, Engine)|  |  Speech Config)|  |
|  +----------+----------+  +---------+----------+  +-------+--------+  |
|             |                       |                     |           |
|             +-----------------------+---------------------+           |
|                                     |                                 |
+-------------------------------------+---------------------------------+
                                      |
         +----------------------------+----------------------------+
         |                                                         |
         v Async Tasks                                             v External API Calls
+-----------------------------------+                     +-----------------------------------+
|        BACKGROUND WORKER          |                     |          EXTERNAL AI SERVICES     |
| (Document Processing, OCR,        |                     | (OpenAI LLM, Embedding API,       |
|  Chunking, Vector Computation)    |                     |  Speech-to-Text, Text-to-Speech)  |
+----------------+------------------+                     +-----------------------------------+
                 |
                 v
+-----------------------------------------------------------------------+
|                           DATA STORAGE TIER                           |
|  +------------------------+ +---------------------+ +--------------+  |
|  | Relational DB          | | Vector Database     | | Object Store |  |
|  | (PostgreSQL / MySQL)   | | (pgvector / Qdrant) | | (S3 / MinIO) |  |
|  +------------------------+ +---------------------+ +--------------+  |
+-----------------------------------------------------------------------+
