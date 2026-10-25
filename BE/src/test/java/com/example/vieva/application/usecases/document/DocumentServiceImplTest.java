package com.example.vieva.application.usecases.document;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.document.CourseDocumentResult;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.FileTypeDetectorPort;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceImplTest {

    private CourseDocumentRepository courseDocumentRepository;
    private DocumentChunkRepository documentChunkRepository;
    private QuestionSourceRepository questionSourceRepository;
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;
    private DocumentParserPort documentParser;
    private FileStoragePort fileStorage;
    private FileTypeDetectorPort fileTypeDetector;
    private TextSplitterPort textSplitter;
    private DomainEventPublisherPort domainEventPublisher;
    private TransactionRunnerPort transactionRunner;
    private JsonSerializerPort jsonSerializer;
    private SubjectAccessGuard subjectAccessGuard;
    private QuestionBankAuditor auditor;
    private DocumentSettings documentSettings;
    private QuestionDraftWriter db;
    private DocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        courseDocumentRepository = mock(CourseDocumentRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        questionSourceRepository = mock(QuestionSourceRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        topicRepository = mock(TopicRepository.class);
        documentParser = mock(DocumentParserPort.class);
        fileStorage = mock(FileStoragePort.class);
        fileTypeDetector = mock(FileTypeDetectorPort.class);
        textSplitter = mock(TextSplitterPort.class);
        domainEventPublisher = mock(DomainEventPublisherPort.class);
        transactionRunner = mock(TransactionRunnerPort.class);
        jsonSerializer = mock(JsonSerializerPort.class);
        subjectAccessGuard = mock(SubjectAccessGuard.class);
        auditor = mock(QuestionBankAuditor.class);
        documentSettings = mock(DocumentSettings.class);
        db = mock(QuestionDraftWriter.class);

        service = new DocumentServiceImpl(
                courseDocumentRepository,
                documentChunkRepository,
                questionSourceRepository,
                subjectRepository,
                topicRepository,
                documentParser,
                fileStorage,
                fileTypeDetector,
                textSplitter,
                domainEventPublisher,
                transactionRunner,
                jsonSerializer,
                subjectAccessGuard,
                auditor,
                documentSettings,
                db
        );
    }

    @Test
    @DisplayName("updateDocumentMetadata: validates topic belongs to subject and updates chunk metadata")
    void updateDocumentMetadata_Success() {
        UUID docId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseDocument doc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .fileName("test.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();

        DocumentChunk chunk = DocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(docId)
                .chunkIndex(1)
                .content("Content")
                .metadataJson("{\"sourceLabel\":\"Trang 1\"}")
                .build();

        when(courseDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(true);
        when(documentChunkRepository.findByDocumentId(docId)).thenReturn(List.of(chunk));
        when(courseDocumentRepository.save(any(CourseDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> initialMap = new HashMap<>();
        initialMap.put("sourceLabel", "Trang 1");
        when(jsonSerializer.deserialize(eq("{\"sourceLabel\":\"Trang 1\"}"), eq(Map.class))).thenReturn(initialMap);
        when(jsonSerializer.serialize(any())).thenAnswer(inv -> "{\"sourceLabel\":\"Trang 1\",\"topicId\":\"" + topicId + "\"}");

        CourseDocumentResult result = service.updateDocumentMetadata(docId, topicId, null, actorId);

        assertThat(result).isNotNull();
        verify(subjectAccessGuard).requireManage(actorId, subjectId);
        verify(topicRepository).existsByIdAndSubjectId(topicId, subjectId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunkCaptor.capture());

        List<DocumentChunk> updatedChunks = chunkCaptor.getValue();
        assertThat(updatedChunks).hasSize(1);
        assertThat(updatedChunks.get(0).getMetadataJson()).contains("topicId");
    }

    @Test
    @DisplayName("updateDocumentMetadata: rejects topic not belonging to subject")
    void updateDocumentMetadata_TopicMismatch_Throws() {
        UUID docId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseDocument doc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .build();

        when(courseDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateDocumentMetadata(docId, topicId, null, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOPIC_NOT_IN_SUBJECT);

        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("softDeleteDocument: throws DOCUMENT_IN_USE when document is cited by question sources")
    void softDeleteDocument_Cited_ThrowsDocumentInUse() {
        UUID docId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseDocument doc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .build();

        when(courseDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(questionSourceRepository.existsByDocumentId(docId)).thenReturn(true);

        assertThatThrownBy(() -> service.softDeleteDocument(docId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_IN_USE);

        verify(courseDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("softDeleteDocument: marks deletedAt when document is not in use")
    void softDeleteDocument_NotInUse_Success() {
        UUID docId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseDocument doc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .fileName("doc.pdf")
                .build();

        when(courseDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(questionSourceRepository.existsByDocumentId(docId)).thenReturn(false);

        service.softDeleteDocument(docId, actorId);

        assertThat(doc.getDeletedAt()).isNotNull();
        verify(courseDocumentRepository).save(doc);
        verify(auditor).record(eq(actorId), eq("DOCUMENT_DELETED"), eq(QuestionBankAuditor.DOCUMENT), eq(docId), any());
    }
}
