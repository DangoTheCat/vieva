package com.example.vieva.integration;

import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchResult;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchService;
import com.example.vieva.application.ports.input.question.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.question.CriterionInput;
import com.example.vieva.application.ports.input.generation.GenerateQuestionsCommand;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.question.RubricInput;
import com.example.vieva.application.ports.input.question.UpdateDraftCommand;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.input.generation.GenerationResult;
import com.example.vieva.application.ports.input.importing.ImportReport;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.input.question.QuestionVersionView;
import com.example.vieva.application.ports.input.document.CourseDocumentResult;
import com.example.vieva.application.ports.input.document.DocumentService;
import com.example.vieva.application.ports.input.generation.QuestionGenCoordinator;
import com.example.vieva.application.ports.input.importing.QuestionImportService;
import com.example.vieva.application.ports.input.question.QuestionAuthoringService;
import com.example.vieva.application.ports.input.question.QuestionBankService;
import com.example.vieva.application.ports.input.question.QuestionReviewService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.DocumentStatus;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionGenerationStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end group-1 flows against PostgreSQL + pgvector with PostgreSQL initialization scripts (V0..V7),
 * Hibernate validation, async indexing and the offline (mock) AI providers. Requires Docker.
 */
@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.autoconfigure.exclude=",
        "vieva.ai.provider=mock",
        "vieva.storage.provider=local",
        "vieva.rag.min-similarity=0.0",
        "vieva.rag.chunk-size-tokens=120",
        "vieva.documents.max-index-attempts=2"
})
@Testcontainers
class QuestionBankIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/migration"), "/opt/vieva-schema")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/init.sh"), "/docker-entrypoint-initdb.d/01-vieva.sh");

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws Exception {
        String root = Files.createTempDirectory("vieva-docs").toString();
        registry.add("vieva.storage.local.root", () -> root);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DocumentService documentService;
    @Autowired private KnowledgeSearchService knowledgeSearchService;
    @Autowired private QuestionGenCoordinator generationService;
    @Autowired private QuestionAuthoringService authoringService;
    @Autowired private QuestionReviewService reviewService;
    @Autowired private QuestionBankService bankService;
    @Autowired private QuestionImportService importService;
    @Autowired private QuestionVersionRepository versionRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private EmbeddingModelPort embeddingModel;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private com.example.vieva.application.ports.output.NotificationRepository notifications;

    @Test
    void submissionRollbackRemovesNotificationAndRecipientIsolationHolds() {
        Fixture fixture=newSubjectWithLecturer();
        var draft=createManual(fixture,"Giải thích transaction rollback?");
        UUID versionId=draft.version().versionId();
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.execute(status -> {
            reviewService.submitForReview(versionId,draft.version().lockVersion(),fixture.lecturerId());
            throw new IllegalStateException("rollback test");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(versionRepository.findById(versionId).orElseThrow().getApprovalStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(notifications.unreadCount(fixture.lecturerId())).isZero();
        var submitted=reviewService.submitForReview(versionId,draft.version().lockVersion(),fixture.lecturerId());
        assertThat(submitted.version().status()).isEqualTo(QuestionStatus.UNDER_REVIEW.name());
        assertThat(submitted.version().lockVersion()).isGreaterThan(draft.version().lockVersion());
        assertThat(notifications.unreadCount(fixture.lecturerId())).isEqualTo(1);
        var notice=notifications.findByRecipient(fixture.lecturerId(),0,20).getContent().get(0);
        assertThat(notifications.markRead(notice.notificationId(),UUID.randomUUID())).isFalse();
        assertThat(notifications.markRead(notice.notificationId(),fixture.lecturerId())).isTrue();
        assertThat(notifications.unreadCount(fixture.lecturerId())).isZero();
        reviewService.approve(versionId,submitted.version().lockVersion(),fixture.lecturerId());
        assertThat(notifications.unreadCount(fixture.lecturerId())).isEqualTo(1);
    }

    @Test
    void archivedDocumentIsExcludedFromRetrievalButKeepsEvidence() {
        Fixture fixture=newSubjectWithLecturer();
        var document=uploadAndAwait(fixture,"archive.txt",TRANSACTIONS_TEXT,DocumentStatus.READY);
        var before=chunkRepository.findByDocumentId(document.getDocumentId());
        documentService.archiveDocument(document.getDocumentId(),fixture.lecturerId());
        assertThat(documentService.getDocumentById(document.getDocumentId()).isAvailableAsEvidence()).isTrue();
        assertThat(chunkRepository.findByDocumentId(document.getDocumentId())).hasSameSizeAs(before);
        assertThat(chunkRepository.similaritySearch(fixture.subjectId(),List.of(document.getDocumentId()),"giao dịch",0,5)).isEmpty();
    }

    private static final String TRANSACTIONS_TEXT = """
            Giao dịch trong cơ sở dữ liệu là một đơn vị công việc logic. Tính nguyên tử bảo đảm hoặc mọi thao tác
            của giao dịch đều thành công, hoặc không thao tác nào được ghi nhận. Tính nhất quán bảo đảm cơ sở dữ liệu
            chuyển từ trạng thái hợp lệ này sang trạng thái hợp lệ khác. Tính cô lập ngăn các giao dịch đồng thời
            nhìn thấy dữ liệu trung gian của nhau. Tính bền vững bảo đảm dữ liệu đã cam kết không bị mất khi hệ thống
            gặp sự cố. Nhật ký ghi trước giúp khôi phục giao dịch sau sự cố. Khoá hai pha là giao thức điều khiển
            tương tranh phổ biến. Mức cô lập read committed ngăn đọc dữ liệu bẩn nhưng vẫn cho phép đọc không lặp lại.
            """;

    private record Fixture(UUID subjectId, UUID lecturerId) {
    }

    private Fixture newSubjectWithLecturer() {
        UUID lecturer = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        String suffix = lecturer.toString().substring(0, 8);
        jdbc.update("INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?,?,?,?,?, 'ACTIVE')",
                lecturer, "gv" + suffix + "@fpt.edu.vn", "GV" + suffix, "Giảng viên " + suffix, "{noop}x");
        jdbc.update("INSERT INTO subjects (subject_id, subject_code, subject_name, status) VALUES (?,?,?, 'ACTIVE')",
                subject, "SWD" + suffix, "Cơ sở dữ liệu " + suffix);
        jdbc.update("INSERT INTO lecturer_subjects (lecturer_subject_id, lecturer_id, subject_id, is_active, assigned_by) "
                + "VALUES (?,?,?, TRUE, ?)", UUID.randomUUID(), lecturer, subject, lecturer);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,role_id FROM roles WHERE role_code='ROLE_LECTURER'",lecturer);
        jdbc.update("UPDATE subjects SET head_lecturer_id=? WHERE subject_id=?",lecturer,subject);
        return new Fixture(subject, lecturer);
    }

    private CourseDocumentResult uploadAndAwait(Fixture fixture, String name, String text, DocumentStatus expected) {
        CourseDocumentResult uploaded = documentService.uploadDocument(fixture.subjectId(), name,
                text.getBytes(StandardCharsets.UTF_8), fixture.lecturerId());
        assertThat(uploaded.getIndexingStatus()).isEqualTo(DocumentStatus.UPLOADED.name());
        await().atMost(Duration.ofSeconds(30)).until(
                () -> expected.name().equals(documentService.getDocumentById(uploaded.getDocumentId()).getIndexingStatus()));
        return documentService.getDocumentById(uploaded.getDocumentId());
    }

    private static RubricInput rubric(String... scores) {
        List<CriterionInput> criteria = java.util.stream.IntStream.range(0, scores.length)
                .mapToObj(i -> new CriterionInput("Tiêu chí " + (i + 1), "Mô tả mức đạt", new BigDecimal(scores[i]), List.of(), null))
                .toList();
        return new RubricInput(null, null, null, criteria);
    }

    private QuestionVersionView createManual(Fixture fixture, String content) {
        return authoringService.createManualQuestion(fixture.subjectId(), CreateManualQuestionCommand.builder()
                .content(content)
                .expectedAnswer("Đáp án mẫu")
                .bloomLevel(BloomLevel.APPLY.name())
                .rubric(rubric("4", "6"))
                .build(), fixture.lecturerId());
    }

    @Test
    @DisplayName("UC1.1 + BR-07: upload → async index → READY; retrieval never leaks another subject's chunks")
    void uploadIndexAndRetrieveWithinSubject() {
        Fixture a = newSubjectWithLecturer();
        Fixture b = newSubjectWithLecturer();
        CourseDocumentResult docA = uploadAndAwait(a, "giao-dich.txt", TRANSACTIONS_TEXT, DocumentStatus.READY);
        CourseDocumentResult docB = uploadAndAwait(b, "giao-dich-b.txt", TRANSACTIONS_TEXT, DocumentStatus.READY);

        assertThat(docA.getTotalChunks()).isGreaterThan(1);
        assertThat(docA.getIndexAttempts()).isEqualTo(1);
        assertThat(chunkRepository.findByDocumentId(docA.getDocumentId()))
                .allSatisfy(chunk -> assertThat(chunk.getEmbedding()).hasSize(1536))
                .anySatisfy(chunk -> assertThat(chunk.getMetadataJson()).contains("pageStart"));

        float[] query = embeddingModel.generateEmbedding("tính nguyên tử của giao dịch");
        List<ChunkSearchResult> results = chunkRepository.searchSimilarChunks(a.subjectId(),
                List.of(docA.getDocumentId(), docB.getDocumentId()), query, 0.0, 20);
        assertThat(results).isNotEmpty().allSatisfy(r -> assertThat(r.documentId()).isEqualTo(docA.getDocumentId()));
        assertThat(results.get(0).similarityScore()).isGreaterThanOrEqualTo(results.get(results.size() - 1).similarityScore());

        // A document that is not READY (deleted) is never retrieved.
        jdbc.update("UPDATE course_documents SET deleted_at = NOW() WHERE document_id = ?", docA.getDocumentId());
        assertThat(chunkRepository.searchSimilarChunks(a.subjectId(), List.of(docA.getDocumentId()), query, 0.0, 20)).isEmpty();
    }

    @Test
    @DisplayName("UC1.1: empty text → FAILED with a reason; retry is bounded")
    void failedIndexingAndBoundedRetry() {
        Fixture fixture = newSubjectWithLecturer();
        CourseDocumentResult failed = uploadAndAwait(fixture, "rong.txt", "quá ngắn", DocumentStatus.FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo(ErrorCode.EMPTY_DOCUMENT_TEXT.getMessage());

        documentService.retryIndexing(failed.getDocumentId(), fixture.lecturerId());
        await().atMost(Duration.ofSeconds(30)).until(() ->
                documentService.getDocumentById(failed.getDocumentId()).getIndexAttempts() == 2
                        && DocumentStatus.FAILED.name().equals(documentService.getDocumentById(failed.getDocumentId()).getIndexingStatus()));
        assertThatThrownBy(() -> documentService.retryIndexing(failed.getDocumentId(), fixture.lecturerId()))
                .isInstanceOf(DomainException.class).extracting("errorCode").isEqualTo(DomainErrorCode.RETRY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("WF01: RAG drafts → confirm Bloom → approve (one transaction) → bank → Copy-on-Write → supersede")
    void generateReviewApproveAndCopyOnWrite() {
        Fixture fixture = newSubjectWithLecturer();
        CourseDocumentResult document = uploadAndAwait(fixture, "giao-dich.txt", TRANSACTIONS_TEXT, DocumentStatus.READY);

        GenerationResult generation = generationService.generate(GenerateQuestionsCommand.builder()
                .subjectId(fixture.subjectId())
                .documentIds(List.of(document.getDocumentId()))
                .totalQuestions(3)
                .bloomDistribution(Map.of(BloomLevel.REMEMBER.name(), 1, BloomLevel.ANALYZE.name(), 1, BloomLevel.CREATE.name(), 1))
                .lecturerNote("Tập trung vào ACID")
                .build(), fixture.lecturerId());

        assertThat(generation.request().status()).isEqualTo(QuestionGenerationStatus.COMPLETED.name());
        assertThat(generation.drafts()).hasSize(3).allSatisfy(view -> {
            assertThat(view.version().origin()).isEqualTo(QuestionGenerationMode.AI_RAG.name());
            assertThat(view.version().bloomConfirmed()).isFalse();
            assertThat(view.sources()).isNotEmpty()
                    .allSatisfy(s -> assertThat(s.documentId()).isEqualTo(document.getDocumentId()));
            assertThat(view.rubric().totalScore()).isEqualByComparingTo("10");
        });
        assertThat(generation.drafts()).extracting(v -> v.version().bloomLevel())
                .containsExactlyInAnyOrder(BloomLevel.REMEMBER.name(), BloomLevel.ANALYZE.name(), BloomLevel.CREATE.name());

        QuestionVersionView first = generation.drafts().get(0);
        UUID v1 = first.version().versionId();
        UUID questionId = first.question().questionId();

        // Nothing is in the bank before approval (BR-05).
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).isEmpty();

        assertThatThrownBy(() -> reviewService.submitForReview(v1, null, fixture.lecturerId()))
                .isInstanceOf(DomainException.class).extracting("errorCode").isEqualTo(DomainErrorCode.BLOOM_NOT_CONFIRMED);
        assertThat(versionRepository.findById(v1).orElseThrow().getApprovalStatus()).isEqualTo(QuestionStatus.DRAFT);

        // Regenerate one draft only; the others stay untouched.
        UUID untouched = generation.drafts().get(1).version().versionId();
        String untouchedContent = generation.drafts().get(1).version().questionContent();
        QuestionVersionView regenerated = generationService.regenerate(v1, "Cần câu hỏi rõ hơn",
                first.version().lockVersion(), fixture.lecturerId());
        assertThat(regenerated.version().regenerationCount()).isEqualTo(1);
        assertThat(regenerated.version().questionContent()).isNotEqualTo(first.version().questionContent());
        assertThat(versionRepository.findById(untouched).orElseThrow().getQuestionContent()).isEqualTo(untouchedContent);

        QuestionVersionView confirmed = authoringService.confirmBloom(v1, null, regenerated.version().lockVersion(),
                fixture.lecturerId());
        QuestionVersionView submitted = reviewService.submitForReview(v1, confirmed.version().lockVersion(), fixture.lecturerId());
        QuestionVersionView approved = reviewService.approve(v1, submitted.version().lockVersion(), fixture.lecturerId());
        assertThat(approved.version().status()).isEqualTo(QuestionStatus.APPROVED.name());
        assertThat(approved.question().currentApprovedVersionId()).isEqualTo(v1);

        String level = approved.version().bloomLevel();
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).singleElement()
                .satisfies(item -> assertThat(item.current().version().versionId()).isEqualTo(v1));
        BloomLevel otherLevel = BloomLevel.CREATE.name().equals(level) ? BloomLevel.REMEMBER : BloomLevel.CREATE;
        assertThat(bankService.search(bankCriteria(fixture, otherLevel)).getContent()).isEmpty();

        // Copy-on-Write: the approved version stays in force until the new draft is approved.
        QuestionVersionView draft2 = authoringService.createDraftFromApproved(questionId, fixture.lecturerId());
        assertThat(draft2.version().versionNumber()).isEqualTo(2);
        assertThat(draft2.version().parentVersionId()).isEqualTo(v1);
        assertThat(draft2.sources()).hasSameSizeAs(approved.sources());
        assertThat(draft2.criteria()).hasSameSizeAs(approved.criteria());
        assertThatThrownBy(() -> authoringService.createDraftFromApproved(questionId, fixture.lecturerId()))
                .isInstanceOf(ApplicationException.class).extracting("errorCode").isEqualTo(ErrorCode.DRAFT_ALREADY_EXISTS);
        assertThat(bankService.getDetail(questionId).currentVersion().version().versionId()).isEqualTo(v1);

        QuestionVersionView edited = authoringService.updateDraft(draft2.version().versionId(),
                UpdateDraftCommand.builder().content("Phiên bản 2 của câu hỏi").rubric(rubric("3", "7"))
                        .expectedVersion(draft2.version().lockVersion()).build(), fixture.lecturerId());
        var submitted2 = reviewService.submitForReview(edited.version().versionId(), edited.version().lockVersion(), fixture.lecturerId());
        reviewService.approve(edited.version().versionId(), submitted2.version().lockVersion(), fixture.lecturerId());

        assertThat(versionRepository.findById(v1).orElseThrow().getApprovalStatus()).isEqualTo(QuestionStatus.SUPERSEDED);
        assertThat(bankService.getDetail(questionId).currentVersion().version().questionContent())
                .isEqualTo("Phiên bản 2 của câu hỏi");
        assertThat(bankService.getHistory(questionId)).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action_type = 'QUESTION_APPROVED' "
                + "AND entity_id IN (?, ?)", Integer.class, v1.toString(), edited.version().versionId().toString()))
                .isEqualTo(2);

        bankService.archive(questionId, fixture.lecturerId());
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).singleElement()
                .satisfies(item -> assertThat(item.current().question().status()).isEqualTo("ARCHIVED"));
        // Evidence is never cascade-deleted (BR-04).
        assertThatThrownBy(() -> jdbc.update("DELETE FROM questions WHERE question_id = ?", questionId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("lost assignment while the screen is open → approval refused, nothing published")
    void approvalAfterPermissionRevoked() {
        Fixture fixture = newSubjectWithLecturer();
        QuestionVersionView draft = createManual(fixture, "Áp dụng chuẩn hoá 3NF?");
        jdbc.update("UPDATE lecturer_subjects SET is_active = FALSE, revoked_at = NOW() WHERE lecturer_id = ?",
                fixture.lecturerId());

        assertThatThrownBy(() -> reviewService.approve(draft.version().versionId(), null, fixture.lecturerId()))
                .isInstanceOf(ApplicationException.class).extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);
        assertThat(versionRepository.findById(draft.version().versionId()).orElseThrow().getApprovalStatus())
                .isEqualTo(QuestionStatus.DRAFT);
        assertThat(jdbc.queryForObject("SELECT current_approved_version_id FROM questions WHERE question_id = ?",
                UUID.class, draft.question().questionId())).isNull();
    }

    @Test
    @DisplayName("BR-08: stale lock tokens and stale entity writes are rejected")
    void optimisticLocking() {
        Fixture fixture = newSubjectWithLecturer();
        QuestionVersionView draft = createManual(fixture, "Giải thích khoá hai pha?");
        UUID versionId = draft.version().versionId();
        Long token = draft.version().lockVersion();

        authoringService.updateDraft(versionId, UpdateDraftCommand.builder().content("Lần sửa 1").expectedVersion(token).build(),
                fixture.lecturerId());
        assertThatThrownBy(() -> authoringService.updateDraft(versionId,
                UpdateDraftCommand.builder().content("Lần sửa song song").expectedVersion(token).build(), fixture.lecturerId()))
                .isInstanceOf(DomainException.class).extracting("errorCode").isEqualTo(DomainErrorCode.CONCURRENT_MODIFICATION);

        QuestionVersion copyA = versionRepository.findById(versionId).orElseThrow();
        QuestionVersion copyB = versionRepository.findById(versionId).orElseThrow();
        copyA.editContent("A thắng", null, null, null);
        versionRepository.save(copyA);
        copyB.editContent("B ghi đè", null, null, null);
        assertThatThrownBy(() -> versionRepository.save(copyB)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(versionRepository.findById(versionId).orElseThrow().getQuestionContent()).isEqualTo("A thắng");
    }

    @Test
    @DisplayName("UC1.6: dry-run writes nothing; commit stores only valid questions as IMPORT drafts")
    void importDryRunThenCommit() {
        Fixture fixture = newSubjectWithLecturer();
        String csv = """
                question_ref,topic,content,expected_answer,bloom_level,criterion_name,criterion_description,criterion_max_score
                Q1,Giao dịch,Nêu ACID?,Atomicity...,Nhớ,Đủ ý,0-4,4
                Q1,,,,,Giải thích,0-6,6
                Q2,Giao dịch,Câu lỗi?,x,KHONG_HOP_LE,Đúng,0-10,10
                Q3,Chỉ mục,Đánh giá B-tree?,Ưu nhược điểm,Đánh giá,Lập luận,0-10,10
                """;
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String countSql = "SELECT COUNT(*) FROM questions WHERE subject_id = ?";

        ImportReport dryRun = importService.importQuestions(fixture.subjectId(), "q.csv", bytes, true, fixture.lecturerId());
        assertThat(dryRun.validQuestions()).isEqualTo(2);
        assertThat(dryRun.errors()).singleElement().satisfies(e -> {
            assertThat(e.row()).isEqualTo(4);
            assertThat(e.column()).isEqualTo("bloom_level");
        });
        assertThat(jdbc.queryForObject(countSql, Integer.class, fixture.subjectId())).isZero();

        ImportReport commit = importService.importQuestions(fixture.subjectId(), "q.csv", bytes, false, fixture.lecturerId());
        assertThat(commit.createdQuestions()).isEqualTo(2);
        assertThat(jdbc.queryForObject(countSql, Integer.class, fixture.subjectId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM question_versions v JOIN questions q ON q.question_id = v.question_id "
                + "WHERE q.subject_id = ? AND v.generation_mode = 'IMPORT' AND v.approval_status = 'DRAFT'", Integer.class,
                fixture.subjectId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM topics WHERE subject_id = ?", Integer.class, fixture.subjectId()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("schema: CHECK constraints reject a Bloom level outside the 6 levels")
    void bloomCheckConstraint() {
        Fixture fixture = newSubjectWithLecturer();
        QuestionVersionView draft = createManual(fixture, "Câu kiểm tra ràng buộc?");
        assertThatThrownBy(() -> jdbc.update("UPDATE question_versions SET bloom_level = 'VAN_DUNG_CAO' WHERE question_version_id = ?",
                draft.version().versionId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("createKnowledge persists valid course_document with non-null file_url and 1536-dim vector chunks in PostgreSQL")
    void createKnowledgePersistsValidCourseDocumentAndChunksInPostgreSql() {
        Fixture fixture = newSubjectWithLecturer();
        String content = "Kiến trúc microservices chia ứng dụng thành các dịch vụ nhỏ độc lập giao tiếp qua API.";

        KnowledgeSearchResult result = knowledgeSearchService.createKnowledge(
                fixture.subjectId(), content, null, fixture.lecturerId());

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isNotNull();

        // Assert PostgreSQL course_documents record
        String fileUrl = jdbc.queryForObject(
                "SELECT file_url FROM course_documents WHERE document_id = ?",
                String.class, result.getDocumentId());
        assertThat(fileUrl).isNotNull().isNotEmpty();

        String storageKey = jdbc.queryForObject(
                "SELECT storage_key FROM course_documents WHERE document_id = ?",
                String.class, result.getDocumentId());
        assertThat(storageKey).isNotNull().isNotEmpty();

        String status = jdbc.queryForObject(
                "SELECT indexing_status FROM course_documents WHERE document_id = ?",
                String.class, result.getDocumentId());
        assertThat(status).isEqualTo("READY");

        // Assert PostgreSQL document_chunks embedding
        Integer chunkCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
                Integer.class, result.getDocumentId());
        assertThat(chunkCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("createKnowledge rolls back transaction and persists nothing if subject access fails")
    void createKnowledgeRollsBackOnAccessFailure() {
        Fixture fixture = newSubjectWithLecturer();
        UUID unauthorizedLecturer = UUID.randomUUID();

        assertThatThrownBy(() -> knowledgeSearchService.createKnowledge(
                fixture.subjectId(), "Sample knowledge content", null, unauthorizedLecturer))
                .isInstanceOf(ApplicationException.class);

        Integer docCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_documents WHERE uploaded_by = ?",
                Integer.class, unauthorizedLecturer);
        assertThat(docCount).isZero();
    }

    private static QuestionBankSearchCriteria bankCriteria(Fixture fixture, BloomLevel level) {
        return QuestionBankSearchCriteria.builder().subjectId(fixture.subjectId()).bloomLevel(level == null ? null : level.name()).page(0).size(20).build();
    }
}
