package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.ports.input.UpdateDraftCommand;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.infrastructure.database.PostgreSQLDatabase;
import com.example.vieva.infrastructure.database.QuestionVersionViewAssembler;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentStatus;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC1.3 / UC1.5 / Copy-on-Write with mocked persistence ports; the permission guard, access loader
 * and approval policy are the real implementations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionReviewAndAuthoringServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionVersionRepository versionRepository;
    @Mock private RubricRepository rubricRepository;
    @Mock private RubricCriterionRepository criterionRepository;
    @Mock private QuestionSourceRepository sourceRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private CourseDocumentRepository documentRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LecturerSubjectRepository lecturerSubjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort jsonSerializer;

    @Mock private com.example.vieva.application.ports.input.access.SubjectHeadService heads;
    @Mock private ReviewNotifications notifications;
    private QuestionReviewServiceImpl reviewService;
    private QuestionAuthoringServiceImpl authoringService;

    private final UUID lecturer = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final Map<UUID, QuestionVersion> versions = new HashMap<>();
    private Question question;

    @BeforeEach
    void setUp() {
        SubjectAccessGuard guard = new SubjectAccessGuard(lecturerSubjectRepository, userRepository);
        QuestionAccessLoader loader = new QuestionAccessLoader(questionRepository, versionRepository, guard);
        QuestionVersionViewAssembler assembler = new QuestionVersionViewAssembler(questionRepository, topicRepository,
                rubricRepository, criterionRepository, sourceRepository);
        QuestionBankAuditor auditor = new QuestionBankAuditor(auditEventRepository, jsonSerializer);
        PostgreSQLDatabase writer = new PostgreSQLDatabase(questionRepository, versionRepository, rubricRepository,
                criterionRepository, sourceRepository, documentRepository, assembler);
        reviewService = new QuestionReviewServiceImpl(questionRepository, versionRepository, rubricRepository,
                criterionRepository, sourceRepository, chunkRepository, documentRepository, loader, assembler, auditor, heads, notifications, writer);
        authoringService = new QuestionAuthoringServiceImpl(questionRepository, versionRepository, rubricRepository,
                criterionRepository, sourceRepository, topicRepository, guard, loader, writer, assembler, auditor);

        question = Question.newDraftOwner(subjectId, null, lecturer);
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(questionRepository.findAllByIds(anyCollection())).thenReturn(List.of(question));
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(true);
        when(versionRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(versions.get(inv.getArgument(0))));
        when(versionRepository.save(any())).thenAnswer(inv -> {
            QuestionVersion saved = inv.getArgument(0);
            versions.put(saved.getQuestionVersionId(), saved);
            return saved;
        });
        when(rubricRepository.findByQuestionVersionIds(anyCollection())).thenReturn(List.of());
    }

    private QuestionVersion storeDraft(QuestionGenerationMode mode, boolean bloomConfirmed) {
        QuestionVersion version = QuestionVersion.newDraft(question.getQuestionId(), 1, "Giải thích ACID?", "ACID là...",
                BloomLevel.UNDERSTAND, mode, lecturer);
        version.setBloomConfirmed(bloomConfirmed);
        version.setVersion(0L);
        versions.put(version.getQuestionVersionId(), version);
        Rubric rubric = Rubric.create(version.getQuestionVersionId(), null, null);
        List<RubricCriterion> criteria = List.of(
                RubricCriterion.create(rubric.getRubricId(), "Đủ ý", "0-10", BigDecimal.TEN, List.of(), 1));
        rubric.recalculateTotal(criteria);
        when(rubricRepository.findByQuestionVersionId(version.getQuestionVersionId())).thenReturn(Optional.of(rubric));
        when(criterionRepository.findByRubricId(rubric.getRubricId())).thenReturn(criteria);
        return version;
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(RuntimeException.class)
                .satisfies(e -> {
                    String actualName;
                    if (e instanceof ApplicationException ae) {
                        actualName = ae.getErrorCode() != null ? ae.getErrorCode().name() : null;
                    } else if (e instanceof DomainException de) {
                        actualName = de.getErrorCode() != null ? de.getErrorCode().name() : null;
                    } else {
                        throw new AssertionError("Expected ApplicationException or DomainException but was: " + e.getClass());
                    }
                    assertThat(actualName).isEqualTo(code.name());
                });
    }

    @Test
    void submitUpdatesStatusBeforeNotificationAndLocksEditing() {
        QuestionVersion draft=storeDraft(QuestionGenerationMode.MANUAL,true);
        UUID head=UUID.randomUUID();
        when(heads.headOf(subjectId)).thenReturn(head);
        reviewService.submitForReview(draft.getQuestionVersionId(),0L,lecturer);
        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionStatus.UNDER_REVIEW);
        var order=org.mockito.Mockito.inOrder(versionRepository,notifications);
        order.verify(versionRepository).save(draft);
        order.verify(notifications).notifyPendingReview(head,question.getQuestionId(),draft.getQuestionVersionId());
        assertCode(() -> authoringService.confirmBloom(draft.getQuestionVersionId(),null,0L,lecturer),ErrorCode.VERSION_NOT_DRAFT);
    }

    @Test
    void draftCannotBeApprovedAndOnlyAuthorCanSubmit() {
        QuestionVersion draft=storeDraft(QuestionGenerationMode.MANUAL,true);
        assertCode(() -> reviewService.approve(draft.getQuestionVersionId(),0L,lecturer),ErrorCode.VERSION_NOT_UNDER_REVIEW);
        UUID other=UUID.randomUUID();
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(other,subjectId)).thenReturn(true);
        assertCode(() -> reviewService.submitForReview(draft.getQuestionVersionId(),0L,other),ErrorCode.NOT_QUESTION_AUTHOR);
        verify(notifications,never()).notifyPendingReview(any(),any(),any());
    }

    @Test
    @DisplayName("approve publishes the version, supersedes the previous one and writes an audit event")
    void approvePublishesAndSupersedes() {
        QuestionVersion previous = QuestionVersion.newDraft(question.getQuestionId(), 1, "cũ", "cũ", BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, lecturer);
        previous.submitForReview(); previous.approve(lecturer);
        versions.put(previous.getQuestionVersionId(), previous);
        question.publish(previous.getQuestionVersionId());
        QuestionVersion draft = storeDraft(QuestionGenerationMode.MANUAL, true);
        draft.submitForReview();

        reviewService.approve(draft.getQuestionVersionId(), 0L, lecturer);

        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionStatus.APPROVED);
        assertThat(previous.getApprovalStatus()).isEqualTo(QuestionStatus.SUPERSEDED);
        assertThat(question.getCurrentApprovedVersionId()).isEqualTo(draft.getQuestionVersionId());
        verify(questionRepository).save(question);
        ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(audit.capture());
        assertThat(audit.getValue().getActionType()).isEqualTo("QUESTION_APPROVED");
    }

    @Test
    @DisplayName("approve is blocked with field errors; nothing is written")
    void approveBlockedByPolicy() {
        QuestionVersion draft = storeDraft(QuestionGenerationMode.AI_RAG, false);
        draft.submitForReview();
        when(sourceRepository.findByQuestionVersionId(draft.getQuestionVersionId())).thenReturn(List.of());

        assertThatThrownBy(() -> reviewService.approve(draft.getQuestionVersionId(), null, lecturer))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException domainEx = (DomainException) e;
                    assertThat(domainEx.getErrorCode()).isEqualTo(DomainErrorCode.BLOOM_NOT_CONFIRMED);
                    assertThat(domainEx.getViolations()).extracting(com.example.vieva.domain.exception.FieldViolation::code)
                            .containsExactly(DomainErrorCode.BLOOM_NOT_CONFIRMED, DomainErrorCode.SOURCE_REQUIRED);
                });
        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionStatus.UNDER_REVIEW);
        verify(versionRepository, never()).save(any());
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("AI draft with grounded source in a READY document is approved")
    void approveAiDraftWithSource() {
        QuestionVersion draft = storeDraft(QuestionGenerationMode.AI_RAG, true);
        draft.submitForReview();
        CourseDocument document = CourseDocument.builder().documentId(UUID.randomUUID())
                .indexingStatus(DocumentStatus.READY).build();
        DocumentChunk chunk = DocumentChunk.builder().chunkId(UUID.randomUUID()).documentId(document.getDocumentId())
                .content("ACID gồm Atomicity, Consistency, Isolation, Durability.").build();
        when(sourceRepository.findByQuestionVersionId(draft.getQuestionVersionId())).thenReturn(List.of(
                QuestionSource.builder().chunkId(chunk.getChunkId()).citationQuote("Atomicity, Consistency").build()));
        when(chunkRepository.findAllByIds(anyCollection())).thenReturn(List.of(chunk));
        when(documentRepository.findAllByIds(anyCollection())).thenReturn(List.of(document));

        reviewService.approve(draft.getQuestionVersionId(), null, lecturer);
        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionStatus.APPROVED);
    }

    @Test
    @DisplayName("lost subject assignment while the screen is open → FORBIDDEN_SUBJECT, nothing published")
    void approveAfterPermissionRevoked() {
        QuestionVersion draft = storeDraft(QuestionGenerationMode.MANUAL, true);
        draft.submitForReview();
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(false);
        when(userRepository.findById(lecturer)).thenReturn(Optional.empty());

        assertCode(() -> reviewService.approve(draft.getQuestionVersionId(), null, lecturer), ErrorCode.FORBIDDEN_SUBJECT);
        assertThat(question.getCurrentApprovedVersionId()).isNull();
        verify(versionRepository, never()).save(any());
    }

    @Test
    void approveRejectsNonDraftStaleAndArchived() {
        QuestionVersion draft = storeDraft(QuestionGenerationMode.MANUAL, true);
        draft.submitForReview();
        assertCode(() -> reviewService.approve(draft.getQuestionVersionId(), 7L, lecturer), ErrorCode.CONCURRENT_MODIFICATION);

        reviewService.reject(draft.getQuestionVersionId(), "Thiếu ví dụ", null, lecturer);
        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionStatus.REJECTED);
        assertCode(() -> reviewService.approve(draft.getQuestionVersionId(), null, lecturer), ErrorCode.VERSION_NOT_UNDER_REVIEW);

        QuestionVersion other = storeDraft(QuestionGenerationMode.MANUAL, true);
        other.submitForReview();
        question.archive();
        assertCode(() -> reviewService.approve(other.getQuestionVersionId(), null, lecturer), ErrorCode.QUESTION_ARCHIVED);
    }

    @Test
    @DisplayName("UC1.5: manual question is saved as a MANUAL DRAFT with computed rubric total")
    void createManualQuestion() {
        List<QuestionVersion> savedVersions = new ArrayList<>();
        when(versionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<QuestionVersion> list = inv.getArgument(0);
            list.forEach(v -> versions.put(v.getQuestionVersionId(), v));
            savedVersions.addAll(list);
            return list;
        });
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(eq(lecturer), any())).thenReturn(true);
        CreateManualQuestionCommand command = CreateManualQuestionCommand.builder()
                .content("Thế nào là 3NF?")
                .expectedAnswer("Không có phụ thuộc bắc cầu")
                .bloomLevel(BloomLevel.REMEMBER)
                .rubric(new RubricInput(null, null, null, List.of(
                        new CriterionInput("Định nghĩa", "0-4", new BigDecimal("4"), List.of(), null),
                        new CriterionInput("Ví dụ", "0-6", new BigDecimal("6"), List.of(), null))))
                .build();

        authoringService.createManualQuestion(subjectId, command, lecturer);

        assertThat(savedVersions).singleElement().satisfies(v -> {
            assertThat(v.getApprovalStatus()).isEqualTo(QuestionStatus.DRAFT);
            assertThat(v.getGenerationMode()).isEqualTo(QuestionGenerationMode.MANUAL);
            assertThat(v.isBloomConfirmed()).isTrue();
        });
        ArgumentCaptor<List<Rubric>> rubrics = ArgumentCaptor.forClass(List.class);
        verify(rubricRepository).saveAll(rubrics.capture());
        assertThat(rubrics.getValue().get(0).getTotalPoints()).isEqualByComparingTo("10");
    }

    @Test
    void createManualQuestionReportsFieldErrors() {
        CreateManualQuestionCommand command = CreateManualQuestionCommand.builder()
                .content(" ")
                .expectedAnswer("x")
                .bloomLevel(BloomLevel.APPLY)
                .rubric(new RubricInput(null, null, new BigDecimal("5"), List.of(
                        new CriterionInput("A", "d", new BigDecimal("4"), List.of(), null))))
                .build();
        assertThatThrownBy(() -> authoringService.createManualQuestion(subjectId, command, lecturer))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getViolations()).extracting(com.example.vieva.application.exception.FieldViolation::field)
                        .containsExactly("content", "rubric.totalScore"));
        verify(versionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Copy-on-Write creates version n+1 from the approved one and copies rubric + sources")
    void copyOnWrite() {
        QuestionVersion approved = storeDraft(QuestionGenerationMode.AI_RAG, true);
        approved.submitForReview(); approved.approve(lecturer);
        question.publish(approved.getQuestionVersionId());
        QuestionVersion rejectedLater = QuestionVersion.newDraft(question.getQuestionId(), 2, "x", "y", BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, lecturer);
        when(versionRepository.hasDraftVersion(question.getQuestionId())).thenReturn(false);
        when(versionRepository.findLatestVersion(question.getQuestionId())).thenReturn(Optional.of(rejectedLater));
        when(sourceRepository.findByQuestionVersionId(approved.getQuestionVersionId())).thenReturn(List.of(
                QuestionSource.builder().questionSourceId(UUID.randomUUID()).chunkId(UUID.randomUUID())
                        .questionVersionId(approved.getQuestionVersionId()).citationQuote("q").build()));

        authoringService.createDraftFromApproved(question.getQuestionId(), lecturer);

        QuestionVersion draft = versions.values().stream()
                .filter(v -> approved.getQuestionVersionId().equals(v.getParentVersionId()))
                .findFirst().orElseThrow();
        assertThat(draft.getVersionNumber()).isEqualTo(3);
        assertThat(draft.isDraft()).isTrue();
        assertThat(approved.getApprovalStatus()).isEqualTo(QuestionStatus.APPROVED);
        ArgumentCaptor<List<QuestionSource>> sources = ArgumentCaptor.forClass(List.class);
        verify(sourceRepository).saveAll(sources.capture());
        assertThat(sources.getValue()).singleElement()
                .satisfies(s -> assertThat(s.getQuestionVersionId()).isEqualTo(draft.getQuestionVersionId()));
        verify(criterionRepository).saveAll(anyList());
    }

    @Test
    void copyOnWriteGuards() {
        assertCode(() -> authoringService.createDraftFromApproved(question.getQuestionId(), lecturer),
                ErrorCode.NO_APPROVED_VERSION);
        question.publish(UUID.randomUUID());
        when(versionRepository.hasDraftVersion(question.getQuestionId())).thenReturn(true);
        assertCode(() -> authoringService.createDraftFromApproved(question.getQuestionId(), lecturer),
                ErrorCode.DRAFT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("editing an APPROVED version directly is refused (must go through Copy-on-Write)")
    void updateApprovedRefused() {
        QuestionVersion approved = storeDraft(QuestionGenerationMode.MANUAL, true);
        approved.submitForReview(); approved.approve(lecturer);
        assertCode(() -> authoringService.updateDraft(approved.getQuestionVersionId(),
                UpdateDraftCommand.builder().content("mới").build(), lecturer), ErrorCode.VERSION_NOT_DRAFT);
    }

    @Test
    @DisplayName("updating a draft replaces its rubric and confirms the Bloom level")
    void updateDraftReplacesRubric() {
        QuestionVersion draft = storeDraft(QuestionGenerationMode.AI_RAG, false);
        UpdateDraftCommand command = UpdateDraftCommand.builder()
                .content("Câu đã sửa")
                .bloomConfirmed(true)
                .rubric(new RubricInput("Mới", null, null, List.of(
                        new CriterionInput("A", "d", new BigDecimal("3"), List.of(), null))))
                .expectedVersion(0L)
                .build();

        authoringService.updateDraft(draft.getQuestionVersionId(), command, lecturer);

        assertThat(draft.getQuestionContent()).isEqualTo("Câu đã sửa");
        assertThat(draft.isBloomConfirmed()).isTrue();
        ArgumentCaptor<Rubric> rubric = ArgumentCaptor.forClass(Rubric.class);
        verify(rubricRepository).save(rubric.capture());
        assertThat(rubric.getValue().getTotalPoints()).isEqualByComparingTo("3");
        verify(criterionRepository).deleteByRubricId(rubric.getValue().getRubricId());
    }
}
