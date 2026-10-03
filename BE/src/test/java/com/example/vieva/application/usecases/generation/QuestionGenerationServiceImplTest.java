package com.example.vieva.application.usecases.generation;

import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate.Citation;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate.Criterion;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.QuestionAccessLoader;
import com.example.vieva.application.usecases.question.QuestionDraftWriter;
import com.example.vieva.application.usecases.question.QuestionVersionViewAssembler;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionGenerationStatus;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionGenerationServiceImplTest {

    @Mock private QuestionGenerationRequestRepository requestRepository;
    @Mock private QuestionVersionRepository versionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private RubricRepository rubricRepository;
    @Mock private RubricCriterionRepository criterionRepository;
    @Mock private QuestionSourceRepository sourceRepository;
    @Mock private CourseDocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private EmbeddingModelPort embedding;
    @Mock private QuestionGenerationPort llm;
    @Mock private LecturerSubjectRepository lecturerSubjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort json;

    private QuestionGenerationServiceImpl service;
    private final RagSettings settings = new RagSettings();
    private final UUID lecturer = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final Map<UUID, QuestionGenerationRequest> requests = new HashMap<>();
    private final Map<UUID, QuestionVersion> versions = new HashMap<>();
    private CourseDocument document;
    private ChunkSearchResult chunk;

    @BeforeEach
    void setUp() {
        TransactionRunnerPort direct = new TransactionRunnerPort() {
            @Override
            public <T> T inNewTransaction(Supplier<T> work) {
                return work.get();
            }
        };
        SubjectAccessGuard guard = new SubjectAccessGuard(lecturerSubjectRepository, userRepository);
        QuestionVersionViewAssembler assembler = new QuestionVersionViewAssembler(questionRepository, topicRepository,
                rubricRepository, criterionRepository, sourceRepository);
        service = new QuestionGenerationServiceImpl(requestRepository, versionRepository, rubricRepository,
                criterionRepository, sourceRepository, documentRepository, chunkRepository, subjectRepository,
                topicRepository, embedding, llm, direct, guard,
                new QuestionAccessLoader(questionRepository, versionRepository, guard),
                new QuestionDraftWriter(questionRepository, versionRepository, rubricRepository, criterionRepository,
                        sourceRepository),
                assembler, new QuestionBankAuditor(auditEventRepository, json), settings);

        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(true);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                Subject.builder().subjectId(subjectId).subjectName("Cơ sở dữ liệu").build()));
        document = CourseDocument.builder().documentId(UUID.randomUUID()).subjectId(subjectId).fileName("csdl.pdf")
                .indexingStatus(DocumentIndexingStatus.READY).build();
        when(documentRepository.findAllByIds(anyCollection())).thenReturn(List.of(document));
        chunk = ChunkSearchResult.of(UUID.randomUUID(), document.getDocumentId(), "csdl.pdf", 1, 2,
                "Giao dịch có bốn tính chất ACID. Chỉ mục B-tree tăng tốc truy vấn.", 0.3);
        when(embedding.generateEmbedding(any())).thenReturn(new float[]{1f});
        when(chunkRepository.searchSimilarChunks(eq(subjectId), anyCollection(), any(), anyDouble(), anyInt()))
                .thenReturn(List.of(chunk));

        when(requestRepository.save(any())).thenAnswer(inv -> {
            QuestionGenerationRequest request = inv.getArgument(0);
            requests.put(request.getGenerationRequestId(), request);
            return request;
        });
        when(requestRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(requests.get(inv.getArgument(0))));
        when(versionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<QuestionVersion> list = inv.getArgument(0);
            list.forEach(v -> versions.put(v.getQuestionVersionId(), v));
            return list;
        });
        when(versionRepository.save(any())).thenAnswer(inv -> {
            QuestionVersion v = inv.getArgument(0);
            versions.put(v.getQuestionVersionId(), v);
            return v;
        });
        when(versionRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(versions.get(inv.getArgument(0))));
        when(versionRepository.findAllByIds(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<UUID> ids = inv.getArgument(0);
            return ids.stream().map(versions::get).toList();
        });
        when(versionRepository.findActiveContentsBySubject(subjectId)).thenReturn(List.of());
    }

    private GenerateQuestionsCommand command(Map<BloomLevel, Integer> distribution, int total) {
        return GenerateQuestionsCommand.builder()
                .subjectId(subjectId)
                .documentIds(List.of(document.getDocumentId()))
                .totalQuestions(total)
                .bloomDistribution(distribution)
                .build();
    }

    private static GeneratedQuestionCandidate candidate(String content, String bloom) {
        return new GeneratedQuestionCandidate(content, "Đáp án theo tài liệu", bloom, BigDecimal.TEN,
                List.of(new Criterion("Đúng", "0-6", new BigDecimal("6")), new Criterion("Rõ", "0-4", new BigDecimal("4"))),
                List.of("C1"), List.of(new Citation("C1", "bốn tính chất ACID")));
    }

    @Test
    @DisplayName("valid candidates become AI DRAFTs with sources; invalid ones are rejected and topped up")
    void generatesDraftsAndRetriesMissingOnes() {
        when(llm.generate(any()))
                .thenReturn(List.of(
                        candidate("Nêu bốn tính chất ACID?", "REMEMBER"),
                        candidate("Câu có Bloom sai?", "VAN_DUNG_CAO"),
                        candidate("Phân tích tính cô lập trong giao dịch?", "ANALYZE")))
                .thenReturn(List.of(candidate("Phân tích vai trò của tính bền vững?", "ANALYZE")));

        GenerationResult result = service.generate(command(Map.of(BloomLevel.REMEMBER, 1, BloomLevel.ANALYZE, 2), 3), lecturer);

        assertThat(result.request().getStatus()).isEqualTo(QuestionGenerationStatus.COMPLETED);
        assertThat(result.request().getGeneratedCount()).isEqualTo(3);
        assertThat(result.request().getRejectedCount()).isEqualTo(1);
        assertThat(result.request().getAttemptCount()).isEqualTo(2);
        assertThat(result.request().getRetrievedChunkIds()).containsExactly(chunk.chunkId());
        assertThat(result.drafts()).hasSize(3).allSatisfy(view -> {
            assertThat(view.version().getGenerationMode()).isEqualTo(QuestionGenerationMode.AI_RAG);
            assertThat(view.version().isDraft()).isTrue();
            assertThat(view.version().isBloomConfirmed()).isFalse();
        });

        ArgumentCaptor<QuestionGenerationPrompt> prompts = ArgumentCaptor.forClass(QuestionGenerationPrompt.class);
        verify(llm, times(2)).generate(prompts.capture());
        assertThat(prompts.getAllValues().get(1).bloomDistribution()).containsOnly(Map.entry(BloomLevel.ANALYZE, 1));

        ArgumentCaptor<List<QuestionSource>> sources = ArgumentCaptor.forClass(List.class);
        verify(sourceRepository).saveAll(sources.capture());
        assertThat(sources.getValue()).hasSize(3).allSatisfy(source -> {
            assertThat(source.getChunkId()).isEqualTo(chunk.chunkId());
            assertThat(source.getDocumentId()).isEqualTo(document.getDocumentId());
            assertThat(source.getCitationQuote()).isEqualTo("bốn tính chất ACID");
        });
    }

    @Test
    @DisplayName("LLM failure on every attempt → request FAILED, no draft, retry budget consumed")
    void llmFailureMarksRequestFailed() {
        when(llm.generate(any())).thenThrow(new AiServiceException("timeout"));
        GenerationResult result = service.generate(command(Map.of(BloomLevel.APPLY, 1), 1), lecturer);
        assertThat(result.request().getStatus()).isEqualTo(QuestionGenerationStatus.FAILED);
        assertThat(result.request().getErrorMessage()).contains("timeout");
        assertThat(result.request().getAttemptCount()).isEqualTo(settings.getAttemptsPerRun());
        assertThat(result.drafts()).isEmpty();
        verify(versionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("partial result can be retried for the missing questions only")
    void retryPartialRequest() {
        settings.setAttemptsPerRun(1);
        when(llm.generate(any())).thenReturn(List.of(candidate("Nêu bốn tính chất ACID?", "REMEMBER")));
        GenerationResult first = service.generate(command(Map.of(BloomLevel.REMEMBER, 1, BloomLevel.CREATE, 1), 2), lecturer);
        assertThat(first.request().getStatus()).isEqualTo(QuestionGenerationStatus.PARTIAL);

        QuestionVersion produced = first.drafts().get(0).version();
        when(versionRepository.findByGenerationRequestId(first.request().getGenerationRequestId()))
                .thenReturn(List.of(produced));
        DocumentChunk stored = DocumentChunk.builder().chunkId(chunk.chunkId()).documentId(document.getDocumentId())
                .chunkIndex(1).content(chunk.content()).build();
        when(chunkRepository.findAllByIds(anyCollection())).thenReturn(List.of(stored));
        when(llm.generate(any())).thenReturn(List.of(candidate("Thiết kế một quy trình đảm bảo ACID mới?", "CREATE")));

        GenerationResult retried = service.retry(first.request().getGenerationRequestId(), lecturer);

        assertThat(retried.request().getStatus()).isEqualTo(QuestionGenerationStatus.COMPLETED);
        assertThat(retried.drafts()).singleElement()
                .satisfies(view -> assertThat(view.version().getBloomLevel()).isEqualTo(BloomLevel.CREATE));
    }

    @Test
    @DisplayName("BR-07: documents must be READY and of the same subject")
    void documentChecks() {
        document.setIndexingStatus(DocumentIndexingStatus.INDEXING);
        assertThatThrownBy(() -> service.generate(command(Map.of(BloomLevel.APPLY, 1), 1), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.DOCUMENT_NOT_READY);

        document.setIndexingStatus(DocumentIndexingStatus.READY);
        document.setSubjectId(UUID.randomUUID());
        assertThatThrownBy(() -> service.generate(command(Map.of(BloomLevel.APPLY, 1), 1), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.DOCUMENT_SUBJECT_MISMATCH);
        verify(llm, never()).generate(any());
    }

    @Test
    void distributionMustMatchTotal() {
        assertThatThrownBy(() -> service.generate(command(Map.of(BloomLevel.APPLY, 1), 2), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_BLOOM_DISTRIBUTION);
    }

    @Test
    @DisplayName("no relevant chunk → INSUFFICIENT_CONTEXT and the request is kept as FAILED")
    void noContext() {
        when(chunkRepository.searchSimilarChunks(any(), anyCollection(), any(), anyDouble(), anyInt())).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(command(Map.of(BloomLevel.APPLY, 1), 1), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_CONTEXT);
        assertThat(requests.values()).singleElement()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(QuestionGenerationStatus.FAILED));
    }

    @Test
    @DisplayName("assignment revoked during the LLM call → drafts are not saved")
    void permissionRevokedDuringGeneration() {
        when(llm.generate(any())).thenAnswer(inv -> {
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(false);
            return List.of(candidate("Nêu bốn tính chất ACID?", "APPLY"));
        });
        assertThatThrownBy(() -> service.generate(command(Map.of(BloomLevel.APPLY, 1), 1), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);
        verify(versionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("regeneration replaces only the target draft, reusing the stored context and feedback")
    void regenerateSingleDraft() {
        QuestionGenerationRequest request = QuestionGenerationRequest.start(subjectId, null, lecturer,
                List.of(document.getDocumentId()),
                com.example.vieva.domain.entities.BloomDistribution.of(Map.of(BloomLevel.APPLY, 1), 1), "note");
        request.recordRetrievedContext(List.of(chunk.chunkId()));
        requests.put(request.getGenerationRequestId(), request);
        Question question = Question.newDraftOwner(subjectId, null, lecturer);
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        QuestionVersion draft = QuestionVersion.newDraft(question.getQuestionId(), 1, "Câu cũ?", "cũ", BloomLevel.APPLY,
                QuestionGenerationMode.AI_RAG, lecturer);
        draft.setGenerationRequestId(request.getGenerationRequestId());
        draft.setVersion(4L);
        versions.put(draft.getQuestionVersionId(), draft);
        when(chunkRepository.findAllByIds(anyCollection())).thenReturn(List.of(DocumentChunk.builder()
                .chunkId(chunk.chunkId()).documentId(document.getDocumentId()).chunkIndex(1).content(chunk.content()).build()));
        when(llm.generate(any())).thenReturn(List.of(candidate("Áp dụng ACID vào hệ thống ngân hàng?", "APPLY")));

        service.regenerate(draft.getQuestionVersionId(), "Cần tình huống thực tế", 4L, lecturer);

        assertThat(draft.getQuestionContent()).isEqualTo("Áp dụng ACID vào hệ thống ngân hàng?");
        assertThat(draft.getRegenerationCount()).isEqualTo(1);
        assertThat(draft.isBloomConfirmed()).isFalse();
        ArgumentCaptor<QuestionGenerationPrompt> prompt = ArgumentCaptor.forClass(QuestionGenerationPrompt.class);
        verify(llm).generate(prompt.capture());
        assertThat(prompt.getValue().previousContent()).isEqualTo("Câu cũ?");
        assertThat(prompt.getValue().lecturerFeedback()).isEqualTo("Cần tình huống thực tế");
        assertThat(prompt.getValue().contextChunks()).singleElement()
                .satisfies(c -> assertThat(c.content()).isEqualTo(chunk.content()));
        verify(versionRepository, never()).saveAll(anyList());
        verify(sourceRepository).deleteByQuestionVersionId(draft.getQuestionVersionId());
    }

    @Test
    void regenerateKeepsDraftWhenNothingValid() {
        QuestionGenerationRequest request = QuestionGenerationRequest.start(subjectId, null, lecturer,
                List.of(document.getDocumentId()),
                com.example.vieva.domain.entities.BloomDistribution.of(Map.of(BloomLevel.APPLY, 1), 1), null);
        request.recordRetrievedContext(List.of(chunk.chunkId()));
        requests.put(request.getGenerationRequestId(), request);
        Question question = Question.newDraftOwner(subjectId, null, lecturer);
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        QuestionVersion draft = QuestionVersion.newDraft(question.getQuestionId(), 1, "Câu cũ?", "cũ", BloomLevel.APPLY,
                QuestionGenerationMode.AI_RAG, lecturer);
        draft.setGenerationRequestId(request.getGenerationRequestId());
        versions.put(draft.getQuestionVersionId(), draft);
        when(chunkRepository.findAllByIds(anyCollection())).thenReturn(List.of(DocumentChunk.builder()
                .chunkId(chunk.chunkId()).documentId(document.getDocumentId()).chunkIndex(1).content(chunk.content()).build()));
        when(llm.generate(any())).thenReturn(new ArrayList<>(List.of(candidate("Sai Bloom", "CREATE"))));

        assertThatThrownBy(() -> service.regenerate(draft.getQuestionVersionId(), null, null, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
        assertThat(draft.getQuestionContent()).isEqualTo("Câu cũ?");

        QuestionVersion manual = QuestionVersion.newDraft(question.getQuestionId(), 2, "x", "y", BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, lecturer);
        versions.put(manual.getQuestionVersionId(), manual);
        assertThatThrownBy(() -> service.regenerate(manual.getQuestionVersionId(), null, null, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.REGENERATION_NOT_SUPPORTED);
    }
}
