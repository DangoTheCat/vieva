# Kế hoạch sửa kiến trúc theo Clean Architecture

Căn cứ: skill `.agents/skills/clean-architecture` (Dependency Rule, Interactor, Request/Response Model, Gateway, Main là composition root).
Phạm vi: backend `BE/src/main/java/com/example/vieva`. Frontend không đổi; hợp đồng JSON giữ nguyên.

## 1. Quy tắc luồng đích

```
                  Vòng 3 — Interface Adapters
   HTTP ──▶ adapters (controller, presenter,        infrastructure (gateway DB/AI/mail,  ◀── DB, Gemini,
            exception handler)                      filter, worker, scheduler)               SMTP, Cloudinary
                  │                                            │
                  ▼  chỉ qua ports/input                       ▼  chỉ qua ports/input hoặc implement ports/output
            Vòng 2 — application (ports/input ◀── usecases ──▶ ports/output)
                  │
                  ▼
            Vòng 1 — domain (entities, services, valueobjects, exception)

   Main: infrastructure/configuration — composition root, được biết mọi lớp để nối dây; không lớp nào phụ thuộc Main.
```

Luồng request chuẩn (không nhảy bậc):

```
HTTP JSON → Controller → Request Model → Input Port (interface) → Interactor → Entity
                                                                     │
                                                                     ├─▶ Output Port (interface) ◀── Gateway → JPA/DB
                                                                     ▼
HTTP JSON ← Presenter ← Response Model ◀──────────────────────────────┘
```

Bảng phụ thuộc cho phép (hàng = lớp gọi, cột = lớp bị gọi):

| Từ \ Đến | domain | application | adapters | infrastructure |
|---|---|---|---|---|
| domain | được | **cấm** | **cấm** | **cấm** |
| application | được | được | **cấm** | **cấm** |
| adapters | **cấm** | chỉ `ports/input`, `exception` | được | **cấm** |
| infrastructure | chỉ gateway `database`, chỉ để map dữ liệu (D1, đã chốt) | chỉ `ports/*`, `exception`, `settings` | **cấm** | được |
| configuration (Main) | được | được | được | được |

Điều cấm cụ thể:
1. Controller không gọi repository, không gọi interactor cụ thể, không đọc/ghi domain entity hay enum domain.
2. Use case không dùng Spring, JPA hay class của adapters/infrastructure.
3. Filter, evaluator, worker, scheduler ở infrastructure không gọi repository để tự quyết nghiệp vụ; chúng gọi input port.
4. Gateway chỉ map dữ liệu, không gọi luật nghiệp vụ của domain.
5. Domain không có setter public; đổi trạng thái chỉ qua method nghiệp vụ.

## 2. Hiện trạng: danh sách vi phạm

| Mã | Vi phạm | Bằng chứng |
|---|---|---|
| V1 | Application dùng Spring | 14 file `@Transactional`, 7 file bắt `DataIntegrityViolationException`, 2 file `org.springframework.util.StringUtils` |
| V2 | Controller nhảy qua use case, gọi thẳng output port | `LecturerStudioController` dùng `QuestionVersionRepository` trong `checkVersion`; `NotificationController` dùng thẳng `NotificationRepository`, tự mở `@Transactional` và trả domain `Notification` |
| V3 | Adapters chạm domain | 13 file import `domain.entities.User`; controller dùng `BloomLevel`, `UserStatus`, `SubjectStatus`, `QuestionStatus`…; presenter map thẳng entity; 16 method use case trả entity (`Subject`, `User`, `Topic`, `LecturerSubject`); 12 request model chứa kiểu domain |
| V4 | Controller phụ thuộc class cụ thể | `DocumentService`, `QuestionGenCoordinator` không có interface |
| V5 | Infrastructure nhảy qua use case | `CourseSecurityEvaluator` gọi 5 repository và tự quyết phân quyền; `JwtAuthenticationFilter` đọc `User`, `UserStatus`; `AsyncDocumentIndexingWorker`, `StaleDocumentCleanupScheduler` gọi `DocumentService` cụ thể; `DataInitializer` ghi `Role` qua `RoleRepository` |
| V6 | Infrastructure gọi domain ngoài gateway | `MockEmbeddingGateway`, `MockQuestionGenerationGateway` dùng `QuestionSimilarity`; `QuestionPromptBuilder` dùng `BloomLevel`; `TikaDocumentParserGateway`, `PoiCsvQuestionSpreadsheetGateway` ném `AppException`; `SecurityConfig`, `GlobalExceptionHandler` dùng `ErrorCode` domain |
| V7 | Response model đặt sai chỗ, chứa kiểu domain | `QuestionDetailView`, `QuestionVersionView`, `QuestionBankItemView`, `AuthResult`, `ImportReport`, `GenerationResult`, `DocumentPreview`, `PagedResult` nằm trong `ports/output`; 5 file chứa kiểu domain. Event `DocumentUploadedEvent`, `AccountCreatedEvent` nằm trong `usecases` |
| V8 | Entity mở setter | 15 entity dùng `@Setter`/`@Data`; 21 lời gọi setter trong application, 44 trong test |
| V9 | Không có kiểm tra tự động | Chưa có ArchUnit; vi phạm chỉ phát hiện bằng mắt |

Ngoài ra `ErrorCode` của domain chứa cả lỗi ứng dụng (`USER_NOT_FOUND`, `UNAUTHENTICATED`…), không phải luật nghiệp vụ.

## 3. Cấu trúc package đích

```
domain/
  entities/          entity, enum; không setter public
  services/          QuestionApprovalPolicy, QuestionSimilarity, ExtractionCheck
  valueobjects/
  exception/         DomainException, DomainErrorCode (chỉ lỗi luật nghiệp vụ)
application/
  ports/input/<feature>/   interface use case + Command/Query + Result (response model)
  ports/input/common/      PageResult, ActorView, ErrorView, ErrorTranslator
  ports/output/            repository, gateway, event, exception của port (DuplicateKeyException…)
  usecases/<feature>/      class implement interface trong ports/input
  exception/               ApplicationException, ErrorCode, ErrorCategory
  transaction/             @UseCaseTransaction (annotation Java thuần)
  settings/
adapters/
  web/controllers, web/request, web/presenters, web/GlobalExceptionHandler
infrastructure/
  database/ (gateway), ai/, security/, service/, configuration/ (Main)
```

Giữ tên interface hiện tại (`SubjectService`, `UserService`…) để giảm thay đổi; chỉ chuyển package (D2, đã chốt).

## 4. Các giai đoạn

Mỗi giai đoạn là một commit/PR riêng, test xanh trước khi sang giai đoạn sau.

### Giai đoạn 0 — Lưới an toàn (V9)
- Thêm test dependency `com.tngtech.archunit:archunit-junit5:1.3.0`.
- Tạo `src/test/java/com/example/vieva/architecture/ArchitectureTest.java` với các luật ở mục 5.
- Bọc luật bằng `FreezingArchRule.freeze(...)` để ghi lại vi phạm hiện có. Build vẫn xanh; vi phạm mới bị chặn; số vi phạm chỉ được giảm.
- Xong khi: `mvn test` xanh, thư mục `archunit_store` liệt kê vi phạm hiện tại.

### Giai đoạn 1 — Application không dùng Spring (V1)
- Tạo `application/transaction/UseCaseTransaction` (thuộc tính `readOnly`).
- Tạo `infrastructure/configuration/UseCaseTransactionConfig`: `TransactionAnnotationParser` đọc `@UseCaseTransaction`, gắn vào `AnnotationTransactionAttributeSource` + `TransactionInterceptor`. Rollback cho `RuntimeException` như `@Transactional`.
- Đổi 14 file từ `@Transactional` sang `@UseCaseTransaction`.
- Tạo `application/ports/output/DuplicateKeyException`. Các `*RepositoryImpl.saveAndFlush` bắt `DataIntegrityViolationException` và ném exception này. 7 use case bắt exception mới.
- Thay `StringUtils.hasText` bằng `String#isBlank`.
- Xong khi: `grep org.springframework application/` trả 0 dòng; bỏ freeze luật R2.

### Giai đoạn 2 — Lỗi đi đúng tầng (V6 phần lỗi)
- **Bước 0 (sửa lỗi do giai đoạn 1 gây ra, commit riêng trước):** `DuplicateKeyException` không được use case bắt đang rơi vào handler `Exception` và trả HTTP 500 `9999`. Trước giai đoạn 1, lỗi này là `DataIntegrityViolationException` và trả 409 (hoặc 400 `USER_EXISTED`). Các chỗ bị ảnh hưởng: `QuestionGenCoordinator` (`questionVersionRepository.save`), `QuestionAuthoringServiceImpl` (2 chỗ `save` ngoài try), `RubricServiceImpl`, lần `saveAllAndFlush` thử lại trong `QuestionImportServiceImpl`. Thêm `@ExceptionHandler(DuplicateKeyException.class)` vào `GlobalExceptionHandler` hiện tại, xử lý giống hệt nhánh `DataIntegrityViolationException` (dựa trên cause). Viết test cho cả 3 nhánh: `uq_users_active_email` → 400 `USER_EXISTED`, `uq_question_draft_per_question` → 409 `DRAFT_ALREADY_EXISTS`, còn lại → 409 `INVALID_REQUEST`.
- Khi chuyển handler sang `adapters/web`, adapters không được import `ports/output` (luật R3). Vì vậy: `DuplicateKeyException` mang thêm `ErrorCode` gợi ý do gateway đặt (gateway đọc tên constraint: `UserRepositoryImpl` → `USER_EXISTED`, `QuestionVersionRepositoryImpl` → `DRAFT_ALREADY_EXISTS`). `ErrorTranslator` dùng mã gợi ý nếu có, nếu không thì trả `CONFLICT` + `INVALID_REQUEST`. Status và mã trả về giữ nguyên như bước 0.
- Domain: `DomainException` + `DomainErrorCode` chỉ gồm mã entity đang ném (`QUESTION_ARCHIVED`, `VERSION_NOT_DRAFT`…).
- Application: `ApplicationException` + `ErrorCode` (giữ nguyên mã `1001`… để `FE/src/utils/errorCodes.js` không đổi) + `ErrorCategory` (`NOT_FOUND`, `CONFLICT`, `FORBIDDEN`, `UNAUTHENTICATED`, `VALIDATION`, `INTERNAL`).
- Input port `ErrorTranslator`: đổi `DomainException` hoặc `ApplicationException` thành `ErrorView(code, message, category)`. Implement trong application.
- Chuyển `GlobalExceptionHandler` sang `adapters/web`; chỉ dùng `ErrorTranslator`, `ErrorView`, map `ErrorCategory` sang HTTP status.
- `TikaDocumentParserGateway`, `PoiCsvQuestionSpreadsheetGateway`, `SecurityConfig` dùng exception/mã lỗi của application.
- Xong khi: chỉ domain và application import `domain.exception`; mã lỗi và HTTP status trả về không đổi.

### Giai đoạn 3 — Controller chỉ gọi input port (V2, V4) [ĐÃ HOÀN THÀNH - 20/10/2026]
- Chuyển toàn bộ 14 interface use case từ `usecases/*` sang `ports/input/<feature>`.
- Tách interface theo D2: `DocumentService`, `QuestionGenCoordinator`, và `SubjectHeadService` thành interface trong `ports/input`; phần code hiện tại chuyển sang `DocumentServiceImpl`, `QuestionGenCoordinatorImpl`, `SubjectHeadServiceImpl` trong `usecases`.
- Bỏ `QuestionVersionRepository` khỏi `LecturerStudioController`. Thêm các overload nhận `questionId` vào `QuestionReviewService` và `QuestionReviewServiceImpl` (submit, review, approve, reject); use case kiểm tra version thuộc đúng question (`QUESTION_VERSION_NOT_FOUND` và `INVALID_REQUEST` "Version belongs to another question"). Giữ nguyên các overload cũ để tương thích ngược.
- Tạo input port `NotificationService` (list, unreadCount, markRead) + `NotificationServiceImpl` trong `usecases/notification`, có `@UseCaseTransaction`. `NotificationController` bỏ `NotificationRepository` và `@Transactional`, chỉ gọi port `NotificationService`.
- Thêm và kích hoạt 2 luật ArchUnit không freeze:
  - R7: `adaptersDoNotDependOnUseCases` — `adapters` cấm phụ thuộc `application.usecases..`.
  - R8: `adaptersDoNotDependOnOutputRepositoriesOrPorts` — `adapters` cấm phụ thuộc `*Repository` hoặc `*Port` từ `ports.output`.
- Xong khi: adapters không import `application.usecases` và các repository/port trong `ports.output`. View model trong `ports.output` còn được dùng đến giai đoạn 4.

### Giai đoạn 4 — Dữ liệu qua ranh giới là model của application (V3, V7)
- Request model: thay field enum domain bằng `String`; use case parse sang enum domain, sai thì ném `ApplicationException(INVALID_REQUEST)`.
- Response model mới: `SubjectResult`, `UserResult`, `TopicResult`, `LecturerSubjectResult`, `CourseDocumentResult`. Thêm `NotificationView`. JSON phải giống hệt JSON đang trả từ entity Notification. Use case map entity sang Result trước khi trả.
- Chuyển `QuestionDetailView`, `QuestionVersionView`, `QuestionBankItemView`, `AuthResult`, `ImportReport`, `GenerationResult`, `DocumentPreview` sang `ports/input`; bỏ kiểu domain bên trong. `PagedResult` thành `ports/input/common/PageResult`.
- Presenter map Result sang DTO JSON hiện tại. Giữ nguyên tên field JSON.
- Principal: `ActorView(id, email, role, mustChangePassword)` trong `ports/input/common`. Controller dùng `@AuthenticationPrincipal ActorView`; cập nhật `CurrentUser`.
- Xong khi: adapters không import `domain`; test controller và presenter xanh với JSON cũ.

### Giai đoạn 5 — Infrastructure chỉ vào qua input port (V5, V6 còn lại)
- Input port `CourseAccessQuery` (`canAccessSubject`, `canAccessDocument`, `canAccessQuestion`, `canAccessVersion`, `canAccessGenerationRequest`), implement trong `usecases/access`, dùng lại `SubjectAccessGuard`. `CourseSecurityEvaluator` chỉ lấy id và role từ `Authentication` rồi gọi port.
- Input port `ResolveActorUseCase`: nhận user id từ token, kiểm tra trạng thái, trả `ActorView`. `JwtAuthenticationFilter` chỉ gọi port này.
- Input port `DocumentIndexingUseCase`, `DocumentCleanupUseCase` cho worker và scheduler (DocumentService đang tạm chứa method cho worker và scheduler, cần tách ra).
- Chuyển `DocumentUploadedEvent`, `AccountCreatedEvent` sang `ports/output/event`.
- Xóa `DataInitializer`: role đã được seed bởi `V10__seed_initial_data.sql` và chỉnh ở `V14`. Trước khi xóa, kiểm tra profile test cũng chạy Flyway.
- `QuestionGenerationPrompt` mang bloom level dạng `String` + mô tả; `QuestionPromptBuilder` bỏ import domain. Mock gateway thay `QuestionSimilarity` bằng hàm tách từ nội bộ.
- Xong khi: trong infrastructure chỉ `database/` và `configuration/` import domain.

### Giai đoạn 6 — Đóng gói entity (V8)
- Bỏ `@Setter`, `@Data` ở 15 entity; giữ `@Getter`, `@Builder`.
- Thay 21 lời gọi setter trong application bằng method nghiệp vụ (`rename`, `deactivate`, `assignHead`, `markReady`, `markFailed`…).
- Mapper dựng entity bằng builder hoặc factory `restore(...)`.
- Sửa 44 lời gọi setter trong test.
- Xong khi: `grep -E "@Setter|@Data" domain/` trả 0 dòng.

### Giai đoạn 7 — Khóa luật
- Bỏ `FreezingArchRule`, tất cả luật chạy chặt; xóa `archunit_store`.
- Chạy `run-tests.ps1`; thử tay trên FE: đăng nhập, tạo môn, upload tài liệu, sinh câu hỏi, duyệt câu hỏi.
- Cập nhật `docs/ONBOARDING.md` với bảng phụ thuộc ở mục 1.

## 5. Luật ArchUnit

```java
@AnalyzeClasses(packages = "com.example.vieva", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    private static final String DOMAIN = "com.example.vieva.domain..";
    private static final String APPLICATION = "com.example.vieva.application..";
    private static final String OUTPUT_PORTS = "com.example.vieva.application.ports.output..";
    private static final String USE_CASES = "com.example.vieva.application.usecases..";
    private static final String ADAPTERS = "com.example.vieva.adapters..";
    private static final String INFRA = "com.example.vieva.infrastructure..";
    private static final String MAIN = "com.example.vieva.infrastructure.configuration..";
    private static final String GATEWAYS_DB = "com.example.vieva.infrastructure.database..";

    // R1: domain không biết vòng ngoài
    @ArchTest
    static final ArchRule domainIsPure = noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    APPLICATION, ADAPTERS, INFRA, "org.springframework..", "jakarta.persistence..");

    // R2: application không biết framework và vòng ngoài
    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses().that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ADAPTERS, INFRA, "org.springframework..", "jakarta.persistence..");

    // R3: adapters chỉ đi qua input port, không nhảy bậc
    @ArchTest
    static final ArchRule adaptersUseInputPortsOnly = noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRA, OUTPUT_PORTS, USE_CASES);

    // R4: infrastructure không gọi interactor cụ thể, không gọi adapters
    @ArchTest
    static final ArchRule infraDoesNotCallInteractors = noClasses().that().resideInAPackage(INFRA)
            .and().resideOutsideOfPackage(MAIN)
            .should().dependOnClassesThat().resideInAnyPackage(USE_CASES, ADAPTERS);

    // R5: ngoài gateway database và Main, infrastructure không chạm domain
    @ArchTest
    static final ArchRule onlyDbGatewaysTouchDomain = noClasses().that().resideInAPackage(INFRA)
            .and().resideOutsideOfPackages(MAIN, GATEWAYS_DB)
            .should().dependOnClassesThat().resideInAPackage(DOMAIN);

    // R6: entity không có setter public
    @ArchTest
    static final ArchRule entitiesHaveNoSetters = noMethods()
            .that().arePublic().and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    // R7: adapters không phụ thuộc vào usecases cụ thể
    @ArchTest
    static final ArchRule adaptersDoNotDependOnUseCases = noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAnyPackage(USE_CASES);

    // R8: adapters không phụ thuộc vào *Repository hoặc *Port trong output ports
    @ArchTest
    static final ArchRule adaptersDoNotDependOnOutputRepositoriesOrPorts = noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat(
                    new com.tngtech.archunit.base.DescribedPredicate<>("are output repositories or ports") {
                        @Override
                        public boolean test(com.tngtech.archunit.core.domain.JavaClass javaClass) {
                            return javaClass.getPackageName().startsWith("com.example.vieva.application.ports.output")
                                    && (javaClass.getSimpleName().endsWith("Repository") || javaClass.getSimpleName().endsWith("Port"));
                        }
                    });
}
```

## 6. Rủi ro và cách giảm

| Rủi ro | Cách giảm |
|---|---|
| FE vỡ vì JSON đổi | Presenter giữ nguyên DTO và tên field; test controller kiểm tra JSON |
| Transaction đổi hành vi | Annotation riêng ánh xạ 1-1 sang `RuleBasedTransactionAttribute`; giữ `readOnly`; test tích hợp hiện có phải xanh |
| `DuplicateKeyException` không bắt được | Chỉ ném từ `saveAndFlush`; kiểm tra 7 chỗ bắt đang dùng `saveAndFlush` |
| Diff lớn, khó review | Mỗi giai đoạn một PR; ArchUnit freeze cho phép sửa dần |
| 33 file test phải sửa theo | Sửa test trong cùng PR với code |

## 7. Quyết định đã chốt (19/10/2026)

- **D1 — Đã chốt:** gateway trong `infrastructure/database` được nhận và trả domain entity qua chữ ký output port.
  - Gateway chỉ map dữ liệu giữa JPA entity và domain entity. Gateway không gọi luật nghiệp vụ của domain.
  - Đây là mẫu Gateway trong skill (`PostgresOrderRepository implements OrderRepository` và map ra `Order`).
  - Ngoài `infrastructure/database` và `infrastructure/configuration` (Main), không lớp nào ở infrastructure được import domain. Luật R5 kiểm tra điều này.
  - Không dùng phương án nghiêm ngặt tuyệt đối (output port dùng model riêng của application), vì phương án đó thêm khoảng 20 model và mỗi bảng phải map 2 lần.
- **D2 — Đã chốt:** khi chuyển interface use case sang `ports/input/<feature>`, giữ tên hiện tại (`SubjectService`, `UserService`, `QuestionReviewService`…).
  - Không đổi sang hậu tố `*UseCase`.
  - Class đang bị controller dùng trực tiếp được tách theo cùng kiểu: tên cũ thành interface, class implement thêm hậu tố `Impl`. Cụ thể `DocumentService` + `DocumentServiceImpl`, `QuestionGenCoordinator` + `QuestionGenCoordinatorImpl`. Controller giữ nguyên kiểu đang inject.
  - Input port mới cho infrastructure giữ tên mô tả việc làm: `CourseAccessQuery`, `ResolveActorUseCase`, `DocumentIndexingUseCase`, `DocumentCleanupUseCase`, `ErrorTranslator`.
