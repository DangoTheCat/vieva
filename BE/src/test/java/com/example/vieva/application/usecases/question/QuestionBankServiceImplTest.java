package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CreateManualQuestionRequest;
import com.example.vieva.application.ports.input.GenerateQuestionsRagRequest;
import com.example.vieva.application.ports.input.RubricCriterionInput;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionVersionRepository questionVersionRepository;

    @Mock
    private QuestionSourceRepository questionSourceRepository;

    @Mock
    private RubricRepository rubricRepository;

    @Mock
    private RubricCriterionRepository rubricCriterionRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingModelPort embeddingModel;

    @Mock
    private AiQuestionGeneratorPort aiQuestionGenerator;

    @InjectMocks
    private QuestionBankServiceImpl questionBankService;

    private UUID subjectId;
    private UUID topicId;
    private UUID lecturerId;
    private Topic sampleTopic;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        topicId = UUID.randomUUID();
        lecturerId = UUID.randomUUID();

        ReflectionTestUtils.setField(questionBankService, "similarityThreshold", 0.60);

        sampleTopic = Topic.builder()
                .topicId(topicId)
                .subjectId(subjectId)
                .topicName("Microservices Design")
                .build();
    }

    @Test
    @DisplayName("createManualQuestion should create Question, draft Version, Rubric, and Criteria when criteria sum matches total points")
    void createManualQuestion_success() {
        CreateManualQuestionRequest request = CreateManualQuestionRequest.builder()
                .topicId(topicId)
                .questionContent("Explain Circuit Breaker pattern.")
                .referenceAnswer("It prevents cascading failures by stopping calls to failing services.")
                .bloomLevel(BloomLevel.ANALYZE)
                .rubricName("Circuit Breaker Rubric")
                .totalPoints(new BigDecimal("10.0"))
                .criteria(List.of(
                        RubricCriterionInput.builder()
                                .criterionName("Definition")
                                .maxPoints(new BigDecimal("4.0"))
                                .build(),
                        RubricCriterionInput.builder()
                                .criterionName("Application & Trade-offs")
                                .maxPoints(new BigDecimal("6.0"))
                                .build()
                ))
                .build();

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(sampleTopic));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVersionRepository.save(any(QuestionVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rubricCriterionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionDetailView view = questionBankService.createManualQuestion(subjectId, request, lecturerId);

        assertThat(view).isNotNull();
        assertThat(view.getQuestion()).isNotNull();
        assertThat(view.getDraftVersion()).isNotNull();
        assertThat(view.getDraftVersion().getApprovalStatus()).isEqualTo(QuestionApprovalStatus.DRAFT);
        assertThat(view.getRubric()).isNotNull();
        assertThat(view.getRubric().getTotalPoints()).isEqualByComparingTo(new BigDecimal("10.0"));
        assertThat(view.getCriteria()).hasSize(2);

        verify(questionRepository).save(any(Question.class));
        verify(questionVersionRepository).save(any(QuestionVersion.class));
        verify(rubricRepository).save(any(Rubric.class));
        verify(rubricCriterionRepository).saveAll(any());
    }

    @Test
    @DisplayName("createManualQuestion should throw INVALID_RUBRIC_TOTAL when criteria sum does not equal totalPoints")
    void createManualQuestion_rubricSumMismatch_throwsException() {
        CreateManualQuestionRequest request = CreateManualQuestionRequest.builder()
                .topicId(topicId)
                .questionContent("Explain Circuit Breaker pattern.")
                .referenceAnswer("Reference answer")
                .bloomLevel(BloomLevel.ANALYZE)
                .rubricName("Fault Tolerance")
                .totalPoints(new BigDecimal("10.0"))
                .criteria(List.of(
                        RubricCriterionInput.builder()
                                .criterionName("Definition")
                                .maxPoints(new BigDecimal("3.0")) // Sum is 3.0 != 10.0
                                .build()
                ))
                .build();

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(sampleTopic));

        assertThatThrownBy(() -> questionBankService.createManualQuestion(subjectId, request, lecturerId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RUBRIC_TOTAL);

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateQuestionsViaRag throws INSUFFICIENT_CONTEXT when no similar chunks found")
    void generateQuestionsViaRag_noChunks_throwsInsufficientContext() {
        GenerateQuestionsRagRequest request = GenerateQuestionsRagRequest.builder()
                .topicId(topicId)
                .bloomLevel(BloomLevel.APPLY)
                .quantity(2)
                .build();

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(sampleTopic));
        when(embeddingModel.generateEmbedding(any())).thenReturn(new float[]{0.1f, 0.2f});
        when(documentChunkRepository.searchSimilarChunks(eq(subjectId), any(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> questionBankService.generateQuestionsViaRag(subjectId, request, lecturerId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_CONTEXT);

        verify(aiQuestionGenerator, never()).generateQuestions(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("createDraftFromApproved succeeds and creates a Copy-on-Write v2 DRAFT")
    void createDraftFromApproved_success() {
        UUID questionId = UUID.randomUUID();
        Question question = Question.builder()
                .questionId(questionId)
                .topicId(topicId)
                .status(QuestionStatus.ACTIVE)
                .build();

        QuestionVersion activeVersion = QuestionVersion.builder()
                .questionVersionId(UUID.randomUUID())
                .questionId(questionId)
                .versionNumber(1)
                .questionContent("Original content")
                .referenceAnswer("Original answer")
                .bloomLevel(BloomLevel.UNDERSTAND)
                .approvalStatus(QuestionApprovalStatus.APPROVED)
                .build();

        Rubric rubric = Rubric.builder()
                .rubricId(UUID.randomUUID())
                .questionVersionId(activeVersion.getQuestionVersionId())
                .rubricName("Rubric v1")
                .totalPoints(new BigDecimal("10.0"))
                .build();

        QuestionVersion draft = QuestionVersion.builder()
                .questionVersionId(UUID.randomUUID())
                .questionId(questionId)
                .versionNumber(2)
                .approvalStatus(QuestionApprovalStatus.DRAFT)
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVersionRepository.hasDraftVersion(questionId)).thenReturn(false);
        when(questionVersionRepository.findActiveApprovedVersion(questionId)).thenReturn(Optional.of(activeVersion));
        when(questionVersionRepository.findLatestDraftVersion(questionId)).thenReturn(Optional.of(draft));
        when(questionVersionRepository.findByQuestionId(questionId)).thenReturn(List.of(activeVersion, draft));
        when(questionVersionRepository.save(any(QuestionVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rubricRepository.findByQuestionVersionId(activeVersion.getQuestionVersionId())).thenReturn(Optional.of(rubric));
        when(rubricCriterionRepository.findByRubricId(rubric.getRubricId())).thenReturn(List.of());
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionDetailView view = questionBankService.createDraftFromApproved(questionId, lecturerId);

        assertThat(view).isNotNull();
        assertThat(view.isHasPendingDraft()).isTrue();
        assertThat(view.getDraftVersion()).isNotNull();
        assertThat(view.getDraftVersion().getVersionNumber()).isEqualTo(2);
        assertThat(view.getDraftVersion().getApprovalStatus()).isEqualTo(QuestionApprovalStatus.DRAFT);
    }

    @Test
    @DisplayName("createDraftFromApproved throws DRAFT_ALREADY_EXISTS when question already has an active DRAFT")
    void createDraftFromApproved_alreadyHasDraft_throwsException() {
        UUID questionId = UUID.randomUUID();
        Question question = Question.builder().questionId(questionId).topicId(topicId).build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVersionRepository.hasDraftVersion(questionId)).thenReturn(true);

        assertThatThrownBy(() -> questionBankService.createDraftFromApproved(questionId, lecturerId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRAFT_ALREADY_EXISTS);

        verify(questionVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveQuestionVersion marks draft to APPROVED and calls supersedeOlderApprovedVersions")
    void approveQuestionVersion_success() {
        UUID questionId = UUID.randomUUID();
        UUID v2DraftId = UUID.randomUUID();

        Question question = Question.builder().questionId(questionId).topicId(topicId).build();

        QuestionVersion v2Draft = QuestionVersion.builder()
                .questionVersionId(v2DraftId)
                .questionId(questionId)
                .versionNumber(2)
                .generationMode(QuestionGenerationMode.MANUAL)
                .approvalStatus(QuestionApprovalStatus.DRAFT)
                .build();

        Rubric rubric = Rubric.builder()
                .rubricId(UUID.randomUUID())
                .questionVersionId(v2DraftId)
                .totalPoints(new BigDecimal("10.0"))
                .build();

        RubricCriterion criterion = RubricCriterion.builder()
                .criterionId(UUID.randomUUID())
                .rubricId(rubric.getRubricId())
                .maxPoints(new BigDecimal("10.0"))
                .build();

        when(questionVersionRepository.findById(v2DraftId)).thenReturn(Optional.of(v2Draft));
        when(rubricRepository.findByQuestionVersionId(v2DraftId)).thenReturn(Optional.of(rubric));
        when(rubricCriterionRepository.findByRubricId(rubric.getRubricId())).thenReturn(List.of(criterion));
        when(questionVersionRepository.save(v2Draft)).thenReturn(v2Draft);

        // Mocks for buildQuestionView
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVersionRepository.findByQuestionId(questionId)).thenReturn(List.of(v2Draft));
        when(questionVersionRepository.findActiveApprovedVersion(questionId)).thenReturn(Optional.of(v2Draft));
        when(questionVersionRepository.findLatestDraftVersion(questionId)).thenReturn(Optional.empty());
        when(questionSourceRepository.findByQuestionVersionId(any())).thenReturn(Collections.emptyList());

        QuestionDetailView view = questionBankService.approveQuestionVersion(questionId, v2DraftId, lecturerId);

        assertThat(view).isNotNull();
        assertThat(v2Draft.getApprovalStatus()).isEqualTo(QuestionApprovalStatus.APPROVED);
        assertThat(v2Draft.getReviewedBy()).isEqualTo(lecturerId);

        verify(questionVersionRepository).save(v2Draft);
        verify(questionVersionRepository).supersedeOlderApprovedVersions(questionId, v2DraftId);
    }

    @Test
    @DisplayName("rejectQuestionVersion sets DRAFT to REJECTED with rejection reason")
    void rejectQuestionVersion_success() {
        UUID questionId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();

        Question question = Question.builder().questionId(questionId).topicId(topicId).build();
        QuestionVersion draft = QuestionVersion.builder()
                .questionVersionId(draftId)
                .questionId(questionId)
                .approvalStatus(QuestionApprovalStatus.DRAFT)
                .build();

        when(questionVersionRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(questionVersionRepository.save(draft)).thenReturn(draft);

        // Mocks for buildQuestionView
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionVersionRepository.findByQuestionId(questionId)).thenReturn(List.of(draft));
        when(questionVersionRepository.findActiveApprovedVersion(questionId)).thenReturn(Optional.empty());
        when(questionVersionRepository.findLatestDraftVersion(questionId)).thenReturn(Optional.of(draft));
        when(rubricRepository.findByQuestionVersionId(draftId)).thenReturn(Optional.empty());
        when(questionSourceRepository.findByQuestionVersionId(draftId)).thenReturn(Collections.emptyList());

        questionBankService.rejectQuestionVersion(questionId, draftId, "Inaccurate reference answer", lecturerId);

        assertThat(draft.getApprovalStatus()).isEqualTo(QuestionApprovalStatus.REJECTED);
        assertThat(draft.getRejectionReason()).isEqualTo("Inaccurate reference answer");
        assertThat(draft.getReviewedBy()).isEqualTo(lecturerId);
        verify(questionVersionRepository).save(draft);
    }
}
