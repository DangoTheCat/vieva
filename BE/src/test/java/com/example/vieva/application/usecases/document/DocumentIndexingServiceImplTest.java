package com.example.vieva.application.usecases.document;

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

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingServiceImplTest {

    @Mock
    private CourseDocumentRepository courseDocumentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentParserPort documentParser;

    @Mock
    private FileStoragePort fileStorage;

    @Mock
    private EmbeddingModelPort embeddingModel;

    @Mock
    private TextSplitterPort textSplitter;

    @Mock
    private DomainEventPublisherPort domainEventPublisher;

    @InjectMocks
    private DocumentIndexingServiceImpl documentIndexingService;

    private UUID subjectId;
    private UUID userId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("uploadDocument successfully uploads file, persists CourseDocument with UPLOADED status, and publishes event")
    void uploadDocument_success() {
        byte[] content = "dummy pdf content".getBytes();
        String filename = "syllabus.pdf";
        String mimeType = "application/pdf";
        String fileUrl = "https://cloudinary.com/files/syllabus.pdf";

        when(fileStorage.uploadFile(eq(filename), eq(content), eq(mimeType))).thenReturn(fileUrl);
        when(courseDocumentRepository.save(any(CourseDocument.class))).thenAnswer(invocation -> {
            CourseDocument doc = invocation.getArgument(0);
            doc.setDocumentId(documentId);
            return doc;
        });

        CourseDocument result = documentIndexingService.uploadDocument(subjectId, filename, content, mimeType, userId);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getFileName()).isEqualTo(filename);
        assertThat(result.getFileUrl()).isEqualTo(fileUrl);
        assertThat(result.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.UPLOADED);
        assertThat(result.getFileSizeBytes()).isEqualTo(content.length);

        verify(fileStorage).uploadFile(filename, content, mimeType);
        verify(courseDocumentRepository).save(any(CourseDocument.class));
        verify(domainEventPublisher).publishDocumentUploaded(any(), any());
    }

    @Test
    @DisplayName("uploadDocument throws INVALID_REQUEST when file content is empty")
    void uploadDocument_emptyContent_throwsException() {
        byte[] emptyContent = new byte[0];
        String filename = "empty.pdf";

        assertThatThrownBy(() -> documentIndexingService.uploadDocument(subjectId, filename, emptyContent, "application/pdf", userId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        verify(fileStorage, never()).uploadFile(any(), any(), any());
        verify(courseDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("indexDocument successfully parses text, generates embeddings, persists chunks and sets READY status")
    void indexDocument_success() {
        byte[] fileBytes = "test file bytes".getBytes();
        CourseDocument doc = CourseDocument.builder()
                .documentId(documentId)
                .subjectId(subjectId)
                .fileName("lecture.pdf")
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .build();

        String extractedText = "This is a detailed lecture about Software Architecture. ".repeat(10);
        List<float[]> embeddings = List.of(new float[]{0.1f, 0.2f, 0.3f});

        when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentParser.extractText(any(InputStream.class))).thenReturn(extractedText);
        when(textSplitter.split(extractedText)).thenReturn(List.of(extractedText));
        when(embeddingModel.generateEmbeddings(any())).thenReturn(embeddings);

        documentIndexingService.indexDocument(documentId, fileBytes);

        verify(courseDocumentRepository, atLeast(2)).save(doc);
        verify(documentChunkRepository).saveAll(any());
        assertThat(doc.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.READY);
        assertThat(doc.getTotalChunks()).isGreaterThan(0);
        assertThat(doc.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("indexDocument marks document FAILED when extracted text has fewer than 100 characters")
    void indexDocument_textTooShort_marksFailed() {
        byte[] fileBytes = "empty pdf".getBytes();
        CourseDocument doc = CourseDocument.builder()
                .documentId(documentId)
                .subjectId(subjectId)
                .fileName("scanned.pdf")
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .build();

        when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentParser.extractText(any(InputStream.class))).thenReturn("Too short");

        documentIndexingService.indexDocument(documentId, fileBytes);

        assertThat(doc.getIndexingStatus()).isEqualTo(DocumentIndexingStatus.FAILED);
        assertThat(doc.getErrorMessage()).isEqualTo(ErrorCode.EMPTY_DOCUMENT_TEXT.getMessage());
        verify(courseDocumentRepository, atLeastOnce()).save(doc);
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("softDeleteDocument invokes repository softDelete and updates document")
    void softDeleteDocument_success() {
        CourseDocument doc = CourseDocument.builder()
                .documentId(documentId)
                .subjectId(subjectId)
                .fileName("lecture.pdf")
                .indexingStatus(DocumentIndexingStatus.READY)
                .build();

        when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        documentIndexingService.softDeleteDocument(documentId, userId);

        verify(courseDocumentRepository).softDelete(documentId);
    }
}
