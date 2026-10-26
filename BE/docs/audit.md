Dưới đây là kết quả **Audit kiến trúc Backend (`BE`)** đối chiếu trực tiếp với bộ tiêu chuẩn trong [`.agents/skills/clean-architecture/SKILL.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/.agents/skills/clean-architecture/SKILL.md) (cùng 6 tài liệu tham chiếu trong `references/`) và kế hoạch kiến trúc nội bộ [`CLEAN_ARCHITECTURE_PLAN.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/docs/CLEAN_ARCHITECTURE_PLAN.md).

---

## 1. Điểm số tổng thể (Quick Diagnostic Scoring)

Theo thang chấm của [`SKILL.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/.agents/skills/clean-architecture/SKILL.md#L18-L20) (7 tiêu chí chẩn đoán nhanh):

| # | Câu hỏi chẩn đoán (Quick Diagnostic) | Trạng thái | Đánh giá thực tế trên Source Code `BE` |
| :-: | :--- | :-: | :--- |
| 1 | **Có thể test business rules mà không cần DB, web server hay framework?** | **ĐẠT (1/1)** | `domain` và `application` có **0 dòng import** Spring, Jakarta Persistence/Validation, Hibernate hay Jackson. Transaction được trừu tượng hóa qua [`@UseCaseTransaction`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…xample/vieva/application/transaction/UseCaseTransaction.java). |
| 2 | **Mọi phụ thuộc mã nguồn (source dependencies) đều hướng vào trong?** | **CHƯA ĐẠT (0/1)** | Có rò rỉ ngược ở các tính năng mới (Oral Exam, Grading, Analytics): `adapters` và `ports/input` lộ kiểu `domain.entities`; `infrastructure/service` gọi thẳng vào `application/usecases` và `ports/output` repositories; `infrastructure/ai` chạm trực tiếp `domain.entities`. |
| 3 | **Có thể thay thế Database mà không chạm vào business logic?** | **ĐẠT (1/1)** | Áp dụng đúng **Two-Model Approach**: tách bạch Domain Entity (`domain/entities/*`) và Persistence Model (`infrastructure/database/*JpaEntity` + `*PersistenceMapper`). Lỗi DB constraint được dịch sang [`DuplicateKeyException`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/application/ports/output/DuplicateKeyException.java) tại Gateway. |
| 4 | **Use Cases có độc lập với cơ chế phân phối (HTTP/Web)?** | **ĐẠT (1/1)** | Không có `MultipartFile`, `HttpServletRequest`, `ResponseEntity` hay `Authentication` trong `application`. |
| 5 | **Framework có bị giới hạn ở vòng ngoài cùng?** | **ĐẠT (1/1)** | Spring Boot, Spring Security, JPA/Hibernate, LangChain4j, Tika, POI, Cloudinary đều nằm gọn trong `adapters` và `infrastructure`. |
| 6 | **Đồ thị phụ thuộc giữa các component có không chu trình (DAG)?** | **ĐẠT (1/1)** | Luồng phụ thuộc một chiều: `adapters` / `infrastructure` $\rightarrow$ `application` $\rightarrow$ `domain`. Giữa `ports/input` và `ports/output` không có phụ thuộc chéo. |
| 7 | **Main (Composition Root) có nối dây (wire) toàn bộ phụ thuộc?** | **ĐẠT (1/1)** | [`ApplicationUseCaseConfiguration`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…structure/configuration/ApplicationUseCaseConfiguration.java) và [`QuestionBankSettingsConfig`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…infrastructure/configuration/QuestionBankSettingsConfig.java) khởi tạo toàn bộ Use Cases; tầng `application` không dùng `@Service`/`@Component`. |

> **Tổng điểm: 6/7 tiêu chí đạt $\rightarrow$ Quy đổi thang điểm Skill: `8.5 / 10`** *(Core sạch và độc lập khỏi Framework/DB, nhưng bị tái xuất hiện các điểm rò rỉ ranh giới sau khi gỡ bỏ ArchUnit test và merge nhánh tính năng AIVES/Oral Exam).*

---

## 2. Chi tiết các vi phạm Clean Architecture hiện có

### Vi phạm 1: Rò rỉ kiểu của `domain.entities` ra `ports/input` và `adapters`
- **Nguyên tắc vi phạm**: Mục 1 & 2 của Skill ([`entities-use-cases.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/.agents/…lean-architecture/references/entities-use-cases.md#L197-L230) — *"Request/Response models: No entity types"*) và Luật R3, R8, R9 trong [`CLEAN_ARCHITECTURE_PLAN.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/docs/CLEAN_ARCHITECTURE_PLAN.md#L214-L256).
- **Bằng chứng cụ thể**:
  - **Tại `adapters` (2 file)**:
    1. [`AdminAnalyticsController.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/adapters/controllers/AdminAnalyticsController.java#L8-L38): import và nhận trực tiếp `@RequestParam AttemptStatus status` từ `domain.entities`.
    2. [`UpdateExamStatusApiRequest.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/controllers/request/UpdateExamStatusApiRequest.java#L3-L8): dùng trực tiếp `ExamStatus` của `domain.entities` trong DTO nhận JSON request.
  - **Tại `application/ports/input` (11 file)**:
    1. [`AdminAnalyticsService.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…lication/ports/input/admin/AdminAnalyticsService.java#L4-L13) (`AttemptStatus`)
    2. [`AdminPerformanceItemView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ation/ports/input/admin/AdminPerformanceItemView.java#L3-L16) (`GradeLevel`)
    3. [`ExamView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…mple/vieva/application/ports/input/exam/ExamView.java#L3-L14) (`ExamStatus`)
4. [`UpdateExamStatusCommand.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…lication/ports/input/exam/UpdateExamStatusCommand.java#L3-L6) (`ExamStatus`)
    5. [`StudentExamView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…eva/application/ports/input/exam/StudentExamView.java#L3-L13) (`AttemptStatus`)
    6. [`StudentScoreView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…va/application/ports/input/exam/StudentScoreView.java#L3-L14) (`GradeLevel`)
    7. [`AttemptGradingDetailView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ion/ports/input/grading/AttemptGradingDetailView.java#L3-L16) (`AttemptStatus`)
    8. [`ExamParticipantView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…lication/ports/input/grading/ExamParticipantView.java#L3-L15) (`AttemptStatus`)
    9. [`LecturerScoreView.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…pplication/ports/input/grading/LecturerScoreView.java#L3-L18) (`GradeLevel`)
    10. [`ExamCommands.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/vieva/application/ports/input/exam/ExamCommands.java#L3-L22) (`ExamStatus` — **dead code** không nơi nào gọi)
    11. [`ExamViews.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ple/vieva/application/ports/input/exam/ExamViews.java#L3-L55) (`ExamStatus`, `GradeLevel` — **dead code** không nơi nào gọi)
- **Cách sửa (Dependency Inversion)**: Đổi các trường `AttemptStatus`, `ExamStatus`, `GradeLevel` trong Request/Command/View ở `ports/input` và `adapters` sang `String`; dùng [`EnumValues.parse(...)`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…om/example/vieva/application/usecases/common/EnumValues.java) bên trong Use Case để parse sang enum của domain; xóa 2 file dư thừa `ExamCommands.java` và `ExamViews.java`.

---

### Vi phạm 2: `infrastructure` nhảy cóc qua Input Port, tự xử lý nghiệp vụ và chạm thẳng Domain / Output Repository
- **Nguyên tắc vi phạm**: Mục 1, 3, 6 của Skill (Humble Object & Plugin Architecture) và Luật R4 (`infraDoesNotCallInteractors`), R5 (`onlyDbGatewaysTouchDomain`).
- **Bằng chứng cụ thể**:
  1. **Worker chứa logic nghiệp vụ & gọi thẳng UseCase/Repository**:
     - [`ExamFinalizationWorker`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/infrastructure/service/ExamFinalizationWorker.java#L28-L137) nằm ở `infrastructure/service` nhưng:
- Phụ thuộc trực tiếp vào [`AttemptGradingService`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…e/vieva/application/usecases/exam/AttemptGradingService.java) (interface đặt sai vị trí trong `application.usecases.exam` thay vì `application.ports.input`).
       - Inject trực tiếp 3 Output Repositories (`ExamAttemptRepository`, `ExamTurnRepository`, `ExamScoreRepository`).
       - Đọc/ghi trực tiếp các Domain Entities (`ExamAttempt`, `ExamScore`, `ExamTurn`, `AttemptStatus`) và tự thực thi chuyển trạng thái máy trạng thái `att.syncSuccess()` (dòng 68), `attempt.syncError()` (dòng 116).
  2. **AI Gateways trong `infrastructure/ai` phụ thuộc trực tiếp vào `domain.entities`**:
     - [`GeminiOralExamAdapter`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/infrastructure/ai/GeminiOralExamAdapter.java#L9-L14) và [`RuleBasedGraderFallback`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/vieva/infrastructure/ai/RuleBasedGraderFallback.java#L8-L13) import trực tiếp `ExamAttempt`, `ExamTurn`, `GradeLevel`, `Rubric`, `RubricCriterion`, `ScoringMode`.
     - Nguyên nhân gốc: Các Output Ports [`AttemptGradingGateway`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/application/ports/output/AttemptGradingGateway.java), [`VivaEvaluationGateway`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/application/ports/output/VivaEvaluationGateway.java), [`RuleBasedGraderPort`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…mple/vieva/application/ports/output/RuleBasedGraderPort.java) và [`AttemptGradingResult`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ple/vieva/application/ports/output/AttemptGradingResult.java) nhận/trả thẳng Domain Entities thay vì DTO của Port (trái ngược với cách [`QuestionGenerationPort`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…e/vieva/application/ports/output/QuestionGenerationPort.java) dùng [`QuestionGenerationPrompt`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…vieva/application/ports/output/QuestionGenerationPrompt.java) và [`GeneratedQuestionCandidate`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…eva/application/ports/output/GeneratedQuestionCandidate.java) độc lập với domain).
- **Cách sửa (Dependency Inversion)**:
- Chuyển toàn bộ logic điều phối chấm điểm/đối soát trạng thái `FINALIZING`/`INTERRUPTED` từ `ExamFinalizationWorker` vào một Input Port Use Case (ví dụ `FinalizeExamAttemptUseCase` trong `ports/input/exam`); `ExamFinalizationWorker` chỉ giữ vai trò Humble Object điều phối async/retry và gọi Input Port với `UUID attemptId` (giống [`AsyncDocumentIndexingWorker`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ieva/infrastructure/service/AsyncDocumentIndexingWorker.java)).
  - Thay tham số Domain Entity trong `VivaEvaluationGateway`, `AttemptGradingGateway`, `RuleBasedGraderPort`, `AttemptGradingResult` bằng các record thuần dữ liệu tại `ports/output`.

---

### Vi phạm 3: Đóng gói Domain Entity (Encapsulation) và phân tán Enterprise Business Rules
- **Nguyên tắc vi phạm**: Mục 2 của Skill ([`entities-use-cases.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/.agents/…/clean-architecture/references/entities-use-cases.md#L94-L99) — *"Public setters on everything: No invariant protection; any code can put entity in invalid state"*) và Luật R6 (`entitiesHaveNoSetters`).
- **Bằng chứng cụ thể**:
  1. **Toàn bộ 20 Domain Entities đều đang mở `@Setter` public**:
     - Các commit `d9769c7`..`fcdb9e0` đã khôi phục `@Setter` trên 13 entity cũ, và các entity mới ([`Exam`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/main/java/com/example/vieva/domain/entities/Exam.java#L11), [`ExamAttempt`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/java/com/example/vieva/domain/entities/ExamAttempt.java#L12), [`ExamScore`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/main/java/com/example/vieva/domain/entities/ExamScore.java#L10), [`ExamTurn`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/main/java/com/example/vieva/domain/entities/ExamTurn.java#L12), [`ExamQuestion`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/java/com/example/vieva/domain/entities/ExamQuestion.java#L9), [`ExamAttemptQuestionSnapshot`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/domain/entities/ExamAttemptQuestionSnapshot.java#L9), [`SpeechSettings`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…va/com/example/vieva/domain/entities/SpeechSettings.java#L10), [`RubricTemplate`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ava/com/example/vieva/domain/entities/RubricTemplate.java#L9)) cũng đều gắn `@Setter`.
  2. **Tầng `application` gọi trực tiếp setter để sửa trạng thái Entity (13 vị trí)**:
- [`LecturerGradingServiceImpl.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…n/usecases/grading/LecturerGradingServiceImpl.java#L434-L466) và [dòng 551–553](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…usecases/grading/LecturerGradingServiceImpl.java#L551-L553): gọi `attempt.setTotalScore(...)`, `attempt.setGradeLevel(...)` (bỏ qua hàm nghiệp vụ `attempt.recordScore(score, grade)` có sẵn, khiến `updatedAt` không được cập nhật) và gọi 5 setter liên tiếp lên `ExamScore` (`setTotalScore`, `setGrade`, `setTeacherScore`, `setTeacherFeedback`, `setTeacherId`).
     - [`DocumentServiceImpl.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ation/usecases/document/DocumentServiceImpl.java#L301-L330): gọi `document.setTopicId(...)`, `chunk.setMetadataJson(...)`, `document.setUpdatedAt(...)`.
     - [`AdminUserServiceImpl.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…lication/usecases/user/AdminUserServiceImpl.java#L207-L218): gọi `user.setEmail(...)`, `user.setUserCode(...)`.
  3. **Luật nghiệp vụ cốt lõi (Enterprise Business Rule) bị lặp lại ở 5 nơi thay vì nằm trong Domain**:
     - Quy tắc xếp loại học lực từ điểm số (`>= 8.5` $\rightarrow$ `EXCELLENT`, `>= 7.0` $\rightarrow$ `GOOD`, `>= 5.0` $\rightarrow$ `AVERAGE`, `< 5.0` $\rightarrow$ `POOR`) không nằm trong [`GradeLevel`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/main/java/com/example/vieva/domain/entities/GradeLevel.java), mà bị viết lặp lại 5 lần tại:
       - [`AttemptGradingServiceImpl.java` (dòng 145–155 & 218–228)](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ation/usecases/exam/AttemptGradingServiceImpl.java#L145-L155)
       - [`LecturerGradingServiceImpl.java` (dòng 706–720)](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…n/usecases/grading/LecturerGradingServiceImpl.java#L706-L720)
       - [`GeminiOralExamAdapter.java` (dòng 233–237)](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…vieva/infrastructure/ai/GeminiOralExamAdapter.java#L233-L237)
       - [`RuleBasedGraderFallback.java` (dòng 152–162)](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…eva/infrastructure/ai/RuleBasedGraderFallback.java#L152-L162)
  4. **Không đồng nhất Exception trong Domain**:
- [`ExamScore.overrideTotal`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ava/com/example/vieva/domain/entities/ExamScore.java#L62-L64) và [`ExamTurn.gradeByTeacher`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…va/com/example/vieva/domain/entities/ExamTurn.java#L100-L102) ném `IllegalArgumentException` thay vì [`DomainException`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…java/com/example/vieva/domain/exception/DomainException.java).

---

### Vi phạm 4: SRP / Use Case Granularity, Presenter thiếu đồng nhất & Mất chốt chặn ArchUnit
- **Use Case quá lớn (Vi phạm SRP & Use Case Granularity — [`entities-use-cases.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/.agents/…lean-architecture/references/entities-use-cases.md#L265-L275) khuyến nghị tối đa 3–4 dependencies mỗi Use Case)**:
  - [`SubmitTurnAnswerUseCaseImpl`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ion/usecases/exam/SubmitTurnAnswerUseCaseImpl.java#L16-L31): **13 dependencies**, ôm đồm từ lưu audio, tìm kiếm vector RAG, chấm điểm AI, kiểm tra giới hạn follow-up, lấy snapshot câu hỏi tiếp theo cho đến phát sự kiện kết thúc bài thi.
  - [`QuestionGenCoordinatorImpl`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…secases/generation/QuestionGenCoordinatorImpl.java#L41-L56): **13 dependencies** (650+ dòng).
  - [`LecturerGradingServiceImpl`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…n/usecases/grading/LecturerGradingServiceImpl.java#L20-L33): **11 dependencies** (721 dòng), trộn lẫn các thao tác Query thống kê (`getCoursesForGrading`, `getExamStats`, `getCourseStats`) và Command chấm điểm/ghi đè điểm (`gradeQuestion`, `overrideScore`).
- **Các Controller mới bỏ qua Presenter**:
- Các controller mới ([`LecturerExamController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ample/vieva/adapters/controllers/LecturerExamController.java), [`LecturerGradingController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/adapters/controllers/LecturerGradingController.java), [`AdminAnalyticsController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ple/vieva/adapters/controllers/AdminAnalyticsController.java), [`KnowledgeController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…/example/vieva/adapters/controllers/KnowledgeController.java), [`SpeechController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…com/example/vieva/adapters/controllers/SpeechController.java), [`StudentScoreController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ample/vieva/adapters/controllers/StudentScoreController.java), [`LecturerRubricTemplateController`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…a/adapters/controllers/LecturerRubricTemplateController.java)) trả trực tiếp Response Model của `ports/input` ra `ResponseEntity<...>` mà không qua Presenter như các controller trước đó.
- **Mất kiểm tra tự động ArchUnit**:
  - Tài liệu [`CLEAN_ARCHITECTURE_PLAN.md`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/docs/CLEAN_ARCHITECTURE_PLAN.md#L65-L68) ghi rằng bộ quy tắc ArchUnit R1–R9 đang chạy 100%, nhưng thực tế dependency `archunit-junit5` trong [`pom.xml`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/pom.xml) và file `ArchitectureTest.java` đã bị xóa ở các commit `16d95fd` / `ec60fa5`.

---

## 3. Đề xuất lộ trình khắc phục để đạt 10/10

1. **Bước 1 — Dọn sạch ranh giới `ports/input` và `adapters` (Sửa Vi phạm 1)**:
   - Xóa 2 file dead code [`ExamCommands.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…example/vieva/application/ports/input/exam/ExamCommands.java) và [`ExamViews.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…om/example/vieva/application/ports/input/exam/ExamViews.java).
   - Đổi `AttemptStatus`, `ExamStatus`, `GradeLevel` trong 9 file `ports/input` và 2 file `adapters` sang `String` (giữ nguyên 100% hợp đồng JSON trả về cho Frontend).
2. **Bước 2 — Đảo chiều phụ thuộc ở `ExamFinalizationWorker` và `infrastructure/ai` (Sửa Vi phạm 2)**:
- Chuyển [`AttemptGradingService`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…e/vieva/application/usecases/exam/AttemptGradingService.java) sang `application/ports/input/exam/` và đưa toàn bộ logic truy vấn/đối soát trạng thái trong [`ExamFinalizationWorker`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ple/vieva/infrastructure/service/ExamFinalizationWorker.java) vào trong Use Case.
   - Đổi chữ ký của [`VivaEvaluationGateway`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/application/ports/output/VivaEvaluationGateway.java), [`AttemptGradingGateway`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…le/vieva/application/ports/output/AttemptGradingGateway.java), [`RuleBasedGraderPort`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…mple/vieva/application/ports/output/RuleBasedGraderPort.java), [`AttemptGradingResult`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/m…ple/vieva/application/ports/output/AttemptGradingResult.java) sang dùng DTO của Output Port để `infrastructure/ai` không còn import `domain.entities`.
3. **Bước 3 — Gom luật `GradeLevel.fromScore(...)` vào Domain & thay thế 13 chỗ gọi setter trong `application` (Sửa Vi phạm 3)**:
   - Đưa hàm tính `GradeLevel.fromScore(BigDecimal)` vào [`GradeLevel.java`](file:///d:/FPTU_SCHOOL/2026/FALL_2026/SWD392/vieva/BE/src/main/java/com/example/vieva/domain/entities/GradeLevel.java) và thêm method nghiệp vụ `ExamScore.applyTeacherRegrade(...)` để xóa các lệnh gọi setter rời rạc trong `LecturerGradingServiceImpl`, `DocumentServiceImpl`, `AdminUserServiceImpl`.
4. **Bước 4 — Khôi phục `ArchitectureTest` (tuỳ chọn)**:
   - Bật lại `ArchitectureTest` để tự động chặn mọi vi phạm ranh giới khi merge code mới.

Bạn có muốn mình tiến hành sửa luôn các vi phạm trên (bắt đầu từ Bước 1, 2, 3) không?