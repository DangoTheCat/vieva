package com.example.vieva.integration;

import com.example.vieva.application.ports.input.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.ports.input.UpdateDraftCommand;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.ImportReport;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.application.usecases.generation.QuestionGenerationService;
import com.example.vieva.application.usecases.importing.QuestionImportService;
import com.example.vieva.application.usecases.question.QuestionAuthoringService;
import com.example.vieva.application.usecases.question.QuestionBankService;
import com.example.vieva.application.usecases.question.QuestionReviewService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionGenerationStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
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
 * End-to-end group-1 flows against PostgreSQL + pgvector with Flyway migrations (V0..V7),
 * Hibernate validation, async indexing and the offline (mock) AI providers. Requires Docker.
 */
@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "vieva.ai.provider=mock",
        "vieva.storage.provider=local",
        "vieva.rag.min-similarity=0.0",
        "vieva.rag.chunk-size-tokens=120",
        "vieva.rag.chunk-overlap-tokens=30",
        "vieva.documents.max-index-attempts=2"
})
@Testcontainers
class QuestionBankIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws Exception {
        String root = Files.createTempDirectory("vieva-docs").toString();
        registry.add("vieva.storage.local.root", () -> root);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DocumentIndexingService documentService;
    @Autowired private QuestionGenerationService generationService;
    @Autowired private QuestionAuthoringService authoringService;
    @Autowired private QuestionReviewService reviewService;
    @Autowired private QuestionBankService bankService;
    @Autowired private QuestionImportService importService;
    @Autowired private QuestionVersionRepository versionRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private EmbeddingModelPort embeddingModel;

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
        return new Fixture(subject, lecturer);
    }

    private CourseDocument uploadAndAwait(Fixture fixture, String name, String text, DocumentIndexingStatus expected) {
        CourseDocument uploaded = documentService.uploadDocument(fixture.subjectId(), name,
                text.getBytes(StandardCharsets.UTF_8), fixture.lecturerId());
        assertThat(uploaded.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.UPLOADED);
        await().atMost(Duration.ofSeconds(30)).until(
                () -> documentService.getDocumentById(uploaded.getDocumentId()).getIndexingStatus() == expected);
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
                .bloomLevel(BloomLevel.APPLY)
                .rubric(rubric("4", "6"))
                .build(), fixture.lecturerId());
    }

    @Test
    @DisplayName("UC1.1 + BR-07: upload → async index → READY; retrieval never leaks another subject's chunks")
    void uploadIndexAndRetrieveWithinSubject() {
        Fixture a = newSubjectWithLecturer();
        Fixture b = newSubjectWithLecturer();
        CourseDocument docA = uploadAndAwait(a, "giao-dich.txt", TRANSACTIONS_TEXT, DocumentIndexingStatus.READY);
        CourseDocument docB = uploadAndAwait(b, "giao-dich-b.txt", TRANSACTIONS_TEXT, DocumentIndexingStatus.READY);

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
        CourseDocument failed = uploadAndAwait(fixture, "rong.txt", "quá ngắn", DocumentIndexingStatus.FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo(ErrorCode.EMPTY_DOCUMENT_TEXT.getMessage());

        documentService.retryIndexing(failed.getDocumentId(), fixture.lecturerId());
        await().atMost(Duration.ofSeconds(30)).until(() ->
                documentService.getDocumentById(failed.getDocumentId()).getIndexAttempts() == 2
                        && documentService.getDocumentById(failed.getDocumentId()).getIndexingStatus() == DocumentIndexingStatus.FAILED);
        assertThatThrownBy(() -> documentService.retryIndexing(failed.getDocumentId(), fixture.lecturerId()))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.RETRY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("WF01: RAG drafts → confirm Bloom → approve (one transaction) → bank → Copy-on-Write → supersede")
    void generateReviewApproveAndCopyOnWrite() {
        Fixture fixture = newSubjectWithLecturer();
        CourseDocument document = uploadAndAwait(fixture, "giao-dich.txt", TRANSACTIONS_TEXT, DocumentIndexingStatus.READY);

        GenerationResult generation = generationService.generate(GenerateQuestionsCommand.builder()
                .subjectId(fixture.subjectId())
                .documentIds(List.of(document.getDocumentId()))
                .totalQuestions(3)
                .bloomDistribution(Map.of(BloomLevel.REMEMBER, 1, BloomLevel.ANALYZE, 1, BloomLevel.CREATE, 1))
                .lecturerNote("Tập trung vào ACID")
                .build(), fixture.lecturerId());

        assertThat(generation.request().getStatus()).isEqualTo(QuestionGenerationStatus.COMPLETED);
        assertThat(generation.drafts()).hasSize(3).allSatisfy(view -> {
            assertThat(view.version().getGenerationMode()).isEqualTo(QuestionGenerationMode.AI_RAG);
            assertThat(view.version().isBloomConfirmed()).isFalse();
            assertThat(view.sources()).isNotEmpty()
                    .allSatisfy(s -> assertThat(s.getDocumentId()).isEqualTo(document.getDocumentId()));
            assertThat(view.rubric().getTotalPoints()).isEqualByComparingTo("10");
        });
        assertThat(generation.drafts()).extracting(v -> v.version().getBloomLevel())
                .containsExactlyInAnyOrder(BloomLevel.REMEMBER, BloomLevel.ANALYZE, BloomLevel.CREATE);

        QuestionVersionView first = generation.drafts().get(0);
        UUID v1 = first.version().getQuestionVersionId();
        UUID questionId = first.question().getQuestionId();

        // Nothing is in the bank before approval (BR-05).
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).isEmpty();

        assertThatThrownBy(() -> reviewService.approve(v1, null, fixture.lecturerId()))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.BLOOM_NOT_CONFIRMED);
        assertThat(versionRepository.findById(v1).orElseThrow().getApprovalStatus()).isEqualTo(QuestionApprovalStatus.DRAFT);

        // Regenerate one draft only; the others stay untouched.
        UUID untouched = generation.drafts().get(1).version().getQuestionVersionId();
        String untouchedContent = generation.drafts().get(1).version().getQuestionContent();
        QuestionVersionView regenerated = generationService.regenerate(v1, "Cần câu hỏi rõ hơn",
                first.version().getVersion(), fixture.lecturerId());
        assertThat(regenerated.version().getRegenerationCount()).isEqualTo(1);
        assertThat(regenerated.version().getQuestionContent()).isNotEqualTo(first.version().getQuestionContent());
        assertThat(versionRepository.findById(untouched).orElseThrow().getQuestionContent()).isEqualTo(untouchedContent);

        QuestionVersionView confirmed = authoringService.confirmBloom(v1, null, regenerated.version().getVersion(),
                fixture.lecturerId());
        QuestionVersionView approved = reviewService.approve(v1, confirmed.version().getVersion(), fixture.lecturerId());
        assertThat(approved.version().getApprovalStatus()).isEqualTo(QuestionApprovalStatus.APPROVED);
        assertThat(approved.question().getCurrentApprovedVersionId()).isEqualTo(v1);

        BloomLevel level = approved.version().getBloomLevel();
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).singleElement()
                .satisfies(item -> assertThat(item.current().version().getQuestionVersionId()).isEqualTo(v1));
        BloomLevel otherLevel = level == BloomLevel.CREATE ? BloomLevel.REMEMBER : BloomLevel.CREATE;
        assertThat(bankService.search(bankCriteria(fixture, otherLevel)).getContent()).isEmpty();

        // Copy-on-Write: the approved version stays in force until the new draft is approved.
        QuestionVersionView draft2 = authoringService.createDraftFromApproved(questionId, fixture.lecturerId());
        assertThat(draft2.version().getVersionNumber()).isEqualTo(2);
        assertThat(draft2.version().getParentVersionId()).isEqualTo(v1);
        assertThat(draft2.sources()).hasSameSizeAs(approved.sources());
        assertThat(draft2.criteria()).hasSameSizeAs(approved.criteria());
        assertThatThrownBy(() -> authoringService.createDraftFromApproved(questionId, fixture.lecturerId()))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.DRAFT_ALREADY_EXISTS);
        assertThat(bankService.getDetail(questionId).currentVersion().version().getQuestionVersionId()).isEqualTo(v1);

        QuestionVersionView edited = authoringService.updateDraft(draft2.version().getQuestionVersionId(),
                UpdateDraftCommand.builder().content("Phiên bản 2 của câu hỏi").rubric(rubric("3", "7"))
                        .expectedVersion(draft2.version().getVersion()).build(), fixture.lecturerId());
        reviewService.approve(edited.version().getQuestionVersionId(), edited.version().getVersion(), fixture.lecturerId());

        assertThat(versionRepository.findById(v1).orElseThrow().getApprovalStatus()).isEqualTo(QuestionApprovalStatus.SUPERSEDED);
        assertThat(bankService.getDetail(questionId).currentVersion().version().getQuestionContent())
                .isEqualTo("Phiên bản 2 của câu hỏi");
        assertThat(bankService.getHistory(questionId)).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action_type = 'QUESTION_APPROVED' "
                + "AND entity_id IN (?, ?)", Integer.class, v1.toString(), edited.version().getQuestionVersionId().toString()))
                .isEqualTo(2);

        bankService.archive(questionId, fixture.lecturerId());
        assertThat(bankService.search(bankCriteria(fixture, null)).getContent()).singleElement()
                .satisfies(item -> assertThat(item.current().question().getStatus().name()).isEqualTo("ARCHIVED"));
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

        assertThatThrownBy(() -> reviewService.approve(draft.version().getQuestionVersionId(), null, fixture.lecturerId()))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);
        assertThat(versionRepository.findById(draft.version().getQuestionVersionId()).orElseThrow().getApprovalStatus())
                .isEqualTo(QuestionApprovalStatus.DRAFT);
        assertThat(jdbc.queryForObject("SELECT current_approved_version_id FROM questions WHERE question_id = ?",
                UUID.class, draft.question().getQuestionId())).isNull();
    }

    @Test
    @DisplayName("BR-08: stale lock tokens and stale entity writes are rejected")
    void optimisticLocking() {
        Fixture fixture = newSubjectWithLecturer();
        QuestionVersionView draft = createManual(fixture, "Giải thích khoá hai pha?");
        UUID versionId = draft.version().getQuestionVersionId();
        Long token = draft.version().getVersion();

        authoringService.updateDraft(versionId, UpdateDraftCommand.builder().content("Lần sửa 1").expectedVersion(token).build(),
                fixture.lecturerId());
        assertThatThrownBy(() -> authoringService.updateDraft(versionId,
                UpdateDraftCommand.builder().content("Lần sửa song song").expectedVersion(token).build(), fixture.lecturerId()))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

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
                draft.version().getQuestionVersionId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private static QuestionBankSearchCriteria bankCriteria(Fixture fixture, BloomLevel level) {
        return QuestionBankSearchCriteria.builder().subjectId(fixture.subjectId()).bloomLevel(level).page(0).size(20).build();
    }
}
