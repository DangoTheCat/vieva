package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.FileTypeDetectorPort;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.StoredFile;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentIndexingServiceImplTest {

    @Mock private CourseDocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private QuestionSourceRepository sourceRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private DocumentParserPort parser;
    @Mock private FileStoragePort storage;
    @Mock private FileTypeDetectorPort typeDetector;
    @Mock private EmbeddingModelPort embedding;
    @Mock private TextSplitterPort splitter;
    @Mock private DomainEventPublisherPort events;
    @Mock private JsonSerializerPort json;
    @Mock private LecturerSubjectRepository lecturerSubjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditEventRepository auditEventRepository;

    private DocumentIndexingServiceImpl service;
    private final RagSettings ragSettings = new RagSettings();
    private final DocumentSettings documentSettings = new DocumentSettings();
    private final UUID lecturer = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final AtomicReference<CourseDocument> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TransactionRunnerPort direct = new TransactionRunnerPort() {
            @Override
            public <T> T inNewTransaction(Supplier<T> work) {
                return work.get();
            }
        };
        ragSettings.setEmbeddingBatchSize(2);
        ragSettings.setEmbeddingDimensions(4);
        documentSettings.setMinTextChars(10);
        service = new DocumentIndexingServiceImpl(documentRepository, chunkRepository, sourceRepository, subjectRepository,
                parser, storage, typeDetector, embedding, splitter, events, direct, json,
                new SubjectAccessGuard(lecturerSubjectRepository, userRepository),
                new QuestionBankAuditor(auditEventRepository, json), documentSettings, ragSettings);

        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(true);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                Subject.builder().subjectId(subjectId).status(SubjectStatus.ACTIVE).build()));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            stored.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(documentRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(documentRepository.findByIdForUpdate(any())).thenAnswer(inv -> Optional.ofNullable(stored.get()));
    }

    private CourseDocument uploadedDocument() {
        CourseDocument document = CourseDocument.builder()
                .documentId(UUID.randomUUID())
                .subjectId(subjectId)
                .uploadedBy(lecturer)
                .fileName("csdl.pdf")
                .storageKey("2026/10/x.pdf")
                .mimeType("application/pdf")
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .indexAttempts(0)
                .build();
        stored.set(document);
        return document;
    }

    @Test
    @DisplayName("upload validates real MIME, stores the file, saves UPLOADED and schedules indexing")
    void uploadSuccess() {
        byte[] content = "%PDF-1.7 ...".getBytes();
        when(typeDetector.detect(content, "Giáo trình.pdf")).thenReturn("application/pdf");
        when(storage.store(eq("Giáo trình.pdf"), eq(content), eq("application/pdf")))
                .thenReturn(new StoredFile("2026/10/k.pdf", "local:2026/10/k.pdf"));

        CourseDocument document = service.uploadDocument(subjectId, "C:\\tmp\\Giáo trình.pdf", content, lecturer);

        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.UPLOADED);
        assertThat(document.getStorageKey()).isEqualTo("2026/10/k.pdf");
        assertThat(document.getFileSizeBytes()).isEqualTo(content.length);
        verify(events).publishDocumentIndexingRequested(document.getDocumentId());
        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("upload rejects a file whose content does not match its extension; nothing is stored")
    void uploadRejectsSpoofedType() {
        byte[] content = "MZ....".getBytes();
        when(typeDetector.detect(any(), anyString())).thenReturn("application/x-msdownload");
        assertThatThrownBy(() -> service.uploadDocument(subjectId, "slides.pptx", content, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        verify(storage, never()).store(any(), any(), any());
        verify(events, never()).publishDocumentIndexingRequested(any());
    }

    @Test
    void uploadRequiresAssignment() {
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(false);
        assertThatThrownBy(() -> service.uploadDocument(subjectId, "a.txt", "hello".getBytes(), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);
    }

    @Test
    @DisplayName("indexing: extract → chunk (with page metadata) → batched embeddings → READY")
    void indexSuccess() {
        CourseDocument document = uploadedDocument();
        when(storage.load("2026/10/x.pdf")).thenReturn(new byte[]{1});
        when(parser.extract(any(), eq("application/pdf"))).thenReturn(List.of(
                new ParsedSection("Chương 1: giao dịch và tính chất ACID.", 1, "Chương 1")));
        when(splitter.split(anyList())).thenReturn(List.of(
                new TextChunk("c1", 10, 1, 1, "Chương 1"), new TextChunk("c2", 10, 1, 2, null),
                new TextChunk("c3", 10, 2, 2, null)));
        when(embedding.generateEmbeddings(anyList())).thenAnswer(inv -> {
            List<String> texts = inv.getArgument(0);
            return texts.stream().map(t -> new float[]{1, 0, 0, 0}).toList();
        });

        service.indexDocument(document.getDocumentId());

        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.READY);
        assertThat(document.getTotalChunks()).isEqualTo(3);
        assertThat(document.getIndexAttempts()).isEqualTo(1);
        verify(embedding, times(2)).generateEmbeddings(anyList()); // batch size 2 → 2 calls for 3 chunks
        ArgumentCaptor<List<DocumentChunk>> chunks = ArgumentCaptor.forClass(List.class);
        verify(chunkRepository).saveAll(chunks.capture());
        assertThat(chunks.getValue()).extracting(DocumentChunk::getChunkIndex).containsExactly(1, 2, 3);
        assertThat(chunks.getValue()).allMatch(c -> c.getEmbedding().length == 4);
    }

    @Test
    @DisplayName("embedding failure → FAILED with reason, partial chunks removed, no READY")
    void embeddingFailure() {
        CourseDocument document = uploadedDocument();
        when(storage.load(any())).thenReturn(new byte[]{1});
        when(parser.extract(any(), any())).thenReturn(List.of(new ParsedSection("Nội dung đủ dài để lập chỉ mục.", null, null)));
        when(splitter.split(anyList())).thenReturn(List.of(new TextChunk("c1", 10, null, null, null)));
        when(embedding.generateEmbeddings(anyList())).thenThrow(new AiServiceException("Embedding provider call failed"));

        service.indexDocument(document.getDocumentId());

        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.FAILED);
        assertThat(document.getErrorMessage()).contains("Embedding service error");
        verify(chunkRepository).deleteByDocumentId(document.getDocumentId());
        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("wrong embedding size is treated as a failure (vector(1536) safety)")
    void embeddingDimensionMismatch() {
        CourseDocument document = uploadedDocument();
        when(storage.load(any())).thenReturn(new byte[]{1});
        when(parser.extract(any(), any())).thenReturn(List.of(new ParsedSection("Nội dung đủ dài để lập chỉ mục.", null, null)));
        when(splitter.split(anyList())).thenReturn(List.of(new TextChunk("c1", 10, null, null, null)));
        when(embedding.generateEmbeddings(anyList())).thenReturn(List.of(new float[3]));

        service.indexDocument(document.getDocumentId());
        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.FAILED);
        assertThat(document.getErrorMessage()).contains("dimensions");
    }

    @Test
    @DisplayName("empty/scanned document → FAILED with EMPTY_DOCUMENT_TEXT reason")
    void emptyText() {
        CourseDocument document = uploadedDocument();
        when(storage.load(any())).thenReturn(new byte[]{1});
        when(parser.extract(any(), any())).thenReturn(List.of(new ParsedSection("   ", 1, null)));

        service.indexDocument(document.getDocumentId());
        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.FAILED);
        assertThat(document.getErrorMessage()).isEqualTo(ErrorCode.EMPTY_DOCUMENT_TEXT.getMessage());
        verify(embedding, never()).generateEmbeddings(anyList());
    }

    @Test
    void indexingSkipsDocumentsNotUploaded() {
        CourseDocument document = uploadedDocument();
        document.setIndexingStatus(DocumentIndexingStatus.READY);
        service.indexDocument(document.getDocumentId());
        verify(storage, never()).load(any());
    }

    @Test
    @DisplayName("retry: FAILED → UPLOADED + new indexing event; limit enforced")
    void retry() {
        documentSettings.setMaxIndexAttempts(2);
        CourseDocument document = uploadedDocument();
        document.startIndexing();
        document.markFailed("x");

        service.retryIndexing(document.getDocumentId(), lecturer);
        assertThat(document.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.UPLOADED);
        verify(events).publishDocumentIndexingRequested(document.getDocumentId());

        document.startIndexing();
        document.markFailed("y");
        assertThatThrownBy(() -> service.retryIndexing(document.getDocumentId(), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.RETRY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("documents cited by questions cannot be deleted")
    void deleteInUse() {
        CourseDocument document = uploadedDocument();
        when(sourceRepository.existsByDocumentId(document.getDocumentId())).thenReturn(true);
        assertThatThrownBy(() -> service.softDeleteDocument(document.getDocumentId(), lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.DOCUMENT_IN_USE);

        when(sourceRepository.existsByDocumentId(document.getDocumentId())).thenReturn(false);
        service.softDeleteDocument(document.getDocumentId(), lecturer);
        assertThat(document.getDeletedAt()).isNotNull();
    }

    @Test
    void staleDocumentsAreFailed() {
        CourseDocument indexing = uploadedDocument();
        indexing.startIndexing();
        when(documentRepository.findStaleIndexingDocuments(any())).thenReturn(new ArrayList<>(List.of(indexing)));
        when(documentRepository.findStaleUploadedDocuments(any())).thenReturn(List.of());
        assertThat(service.failStaleDocuments(java.time.Instant.now(), java.time.Instant.now())).isEqualTo(1);
        assertThat(indexing.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.FAILED);
    }
}
