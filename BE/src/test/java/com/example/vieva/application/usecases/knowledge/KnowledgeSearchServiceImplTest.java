package com.example.vieva.application.usecases.knowledge;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchResult;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.StoredFile;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSearchServiceImplTest {

    private CourseDocumentRepository courseDocumentRepository;
    private DocumentChunkRepository documentChunkRepository;
    private EmbeddingModelPort embeddingModel;
    private SubjectAccessGuard subjectAccessGuard;
    private TopicRepository topicRepository;
    private TextSplitterPort textSplitter;
    private FileStoragePort fileStoragePort;
    private KnowledgeSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        courseDocumentRepository = mock(CourseDocumentRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        embeddingModel = mock(EmbeddingModelPort.class);
        subjectAccessGuard = mock(SubjectAccessGuard.class);
        topicRepository = mock(TopicRepository.class);
        textSplitter = mock(TextSplitterPort.class);
        fileStoragePort = mock(FileStoragePort.class);

        when(fileStoragePort.store(any(), any(), any()))
                .thenReturn(new StoredFile("knowledge/mock.txt", "local:knowledge/mock.txt"));

        service = new KnowledgeSearchServiceImpl(
                courseDocumentRepository,
                documentChunkRepository,
                embeddingModel,
                subjectAccessGuard,
                topicRepository,
                textSplitter,
                fileStoragePort
        );
    }

    @Test
    @DisplayName("createKnowledge: successfully creates document and chunk with embedding")
    void createKnowledgeSuccess() {
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String content = "Software architecture involves high-level structures of software systems.";
        float[] fakeEmbedding = new float[1536];
        fakeEmbedding[0] = 0.1f;
        fakeEmbedding[1] = 0.2f;

        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(true);
        when(textSplitter.split(anyList())).thenReturn(List.of(
                new TextChunk(content, 10, 1, 1, "Direct Knowledge")
        ));
        when(embeddingModel.generateEmbedding(content.trim())).thenReturn(fakeEmbedding);

        KnowledgeSearchResult result = service.createKnowledge(subjectId, content, topicId, actorId);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getChunkId()).isNotNull();
        assertThat(result.getDocumentId()).isNotNull();

        verify(subjectAccessGuard).requireManage(actorId, subjectId);

        ArgumentCaptor<CourseDocument> docCaptor = ArgumentCaptor.forClass(CourseDocument.class);
        verify(courseDocumentRepository).save(docCaptor.capture());
        CourseDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getFileUrl()).isEqualTo("local:knowledge/mock.txt");
        assertThat(savedDoc.getStorageKey()).isEqualTo("knowledge/mock.txt");
        assertThat(savedDoc.getTotalChunks()).isEqualTo(1);
        assertThat(savedDoc.getIndexingStatus()).isEqualTo(DocumentStatus.READY);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunkCaptor.capture());
        List<DocumentChunk> savedChunks = chunkCaptor.getValue();
        assertThat(savedChunks).hasSize(1);
        assertThat(savedChunks.get(0).getContent()).isEqualTo(content);
        assertThat(savedChunks.get(0).getEmbedding()).isEqualTo(fakeEmbedding);
    }

    @Test
    @DisplayName("createKnowledge: successfully splits multichunk Unicode text and verifies valid stored fileUrl")
    void createKnowledgeMultiChunkUnicode() {
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String content = "Kiến trúc phần mềm là cấu trúc tổ chức của hệ thống bao gồm các thành phần phần mềm. Tiêu chí đánh giá chất lượng hệ thống bao gồm tính bảo trì, khả năng mở rộng.";

        TextChunk chunk1 = new TextChunk("Kiến trúc phần mềm là cấu trúc tổ chức của hệ thống bao gồm các thành phần phần mềm.", 20, 1, 1, "Direct Knowledge");
        TextChunk chunk2 = new TextChunk("Tiêu chí đánh giá chất lượng hệ thống bao gồm tính bảo trì, khả năng mở rộng.", 18, 1, 1, "Direct Knowledge");

        float[] embedding1 = new float[1536];
        embedding1[0] = 0.1f;
        float[] embedding2 = new float[1536];
        embedding2[0] = 0.3f;

        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(true);
        when(textSplitter.split(anyList())).thenReturn(List.of(chunk1, chunk2));
        when(embeddingModel.generateEmbedding(chunk1.text())).thenReturn(embedding1);
        when(embeddingModel.generateEmbedding(chunk2.text())).thenReturn(embedding2);

        KnowledgeSearchResult result = service.createKnowledge(subjectId, content, topicId, actorId);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(chunk1.text());
        assertThat(result.getChunkIndex()).isEqualTo(0);

        ArgumentCaptor<CourseDocument> docCaptor = ArgumentCaptor.forClass(CourseDocument.class);
        verify(courseDocumentRepository).save(docCaptor.capture());
        CourseDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getFileUrl()).isEqualTo("local:knowledge/mock.txt");
        assertThat(savedDoc.getStorageKey()).isEqualTo("knowledge/mock.txt");
        assertThat(savedDoc.getTotalChunks()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunkCaptor.capture());
        List<DocumentChunk> savedChunks = chunkCaptor.getValue();
        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks.get(0).getContent()).isEqualTo(chunk1.text());
        assertThat(savedChunks.get(0).getEmbedding()).isEqualTo(embedding1);
        assertThat(savedChunks.get(1).getContent()).isEqualTo(chunk2.text());
        assertThat(savedChunks.get(1).getEmbedding()).isEqualTo(embedding2);
    }

    @Test
    @DisplayName("createKnowledge: fails atomically when embedding generation throws exception")
    void createKnowledgeEmbeddingFailureRollsBack() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String content = "Text needing embedding";

        when(textSplitter.split(anyList())).thenReturn(List.of(
                new TextChunk(content, 5, 1, 1, "Direct Knowledge")
        ));
        when(embeddingModel.generateEmbedding(any())).thenThrow(new RuntimeException("Embedding model offline"));

        assertThatThrownBy(() -> service.createKnowledge(subjectId, content, null, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Embedding model offline");

        verify(fileStoragePort).delete("knowledge/mock.txt");
        verify(courseDocumentRepository, never()).save(any());
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createKnowledge: rejects embedding with invalid dimensions (not 1536)")
    void createKnowledgeInvalidEmbeddingDimensionsFails() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String content = "Some content";

        when(textSplitter.split(anyList())).thenReturn(List.of(
                new TextChunk(content, 3, 1, 1, "Direct Knowledge")
        ));
        // Wrong dimension: 128 instead of 1536
        when(embeddingModel.generateEmbedding(any())).thenReturn(new float[128]);

        assertThatThrownBy(() -> service.createKnowledge(subjectId, content, null, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);

        verify(fileStoragePort).delete("knowledge/mock.txt");
        verify(courseDocumentRepository, never()).save(any());
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createKnowledge: rejects null or empty embedding dimension")
    void createKnowledgeInvalidEmbeddingFails() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String content = "Some content";

        when(textSplitter.split(anyList())).thenReturn(List.of(
                new TextChunk(content, 3, 1, 1, "Direct Knowledge")
        ));
        when(embeddingModel.generateEmbedding(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createKnowledge(subjectId, content, null, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);

        verify(fileStoragePort).delete("knowledge/mock.txt");
        verify(courseDocumentRepository, never()).save(any());
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createKnowledge: rejects blank content")
    void createKnowledgeRejectsBlankContent() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        assertThatThrownBy(() -> service.createKnowledge(subjectId, "   ", null, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(courseDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createKnowledge: rejects mismatched topic")
    void createKnowledgeRejectsMismatchedTopic() {
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(false);

        assertThatThrownBy(() -> service.createKnowledge(subjectId, "Valid content", topicId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);

        verify(courseDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createKnowledge: enforces access guard")
    void createKnowledgeEnforcesAccessGuard() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        doThrow(new ApplicationException(ErrorCode.FORBIDDEN_SUBJECT))
                .when(subjectAccessGuard).requireManage(actorId, subjectId);

        assertThatThrownBy(() -> service.createKnowledge(subjectId, "Valid content", null, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);
    }

    @Test
    @DisplayName("listReadyChunks: lists chunks from READY documents up to limit")
    void listReadyChunksReturnsReadyOnly() {
        UUID subjectId = UUID.randomUUID();
        UUID readyDocId = UUID.randomUUID();
        UUID failedDocId = UUID.randomUUID();

        CourseDocument readyDoc = CourseDocument.builder()
                .documentId(readyDocId)
                .subjectId(subjectId)
                .fileName("ready.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();

        CourseDocument failedDoc = CourseDocument.builder()
                .documentId(failedDocId)
                .subjectId(subjectId)
                .fileName("failed.pdf")
                .indexingStatus(DocumentStatus.FAILED)
                .build();

        when(courseDocumentRepository.findActiveBySubjectId(subjectId))
                .thenReturn(List.of(readyDoc, failedDoc));

        DocumentChunk chunk1 = DocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(readyDocId)
                .chunkIndex(0)
                .content("Ready chunk 1")
                .build();

        when(documentChunkRepository.findByDocumentId(readyDocId))
                .thenReturn(List.of(chunk1));

        List<KnowledgeSearchResult> results = service.listReadyChunks(subjectId, 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Ready chunk 1");
        verify(documentChunkRepository, never()).findByDocumentId(failedDocId);
    }

    @Test
    @DisplayName("searchKnowledge: queries embedding and similarity search")
    void searchKnowledgeReturnsSimilarChunks() {
        UUID subjectId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        CourseDocument readyDoc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .fileName("course.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();

        when(courseDocumentRepository.findActiveBySubjectId(subjectId))
                .thenReturn(List.of(readyDoc));

        float[] fakeEmbedding = new float[]{0.5f, 0.5f};
        when(embeddingModel.generateEmbedding("design pattern")).thenReturn(fakeEmbedding);

        ChunkSearchResult chunkResult = ChunkSearchResult.of(
                UUID.randomUUID(),
                docId,
                "course.pdf",
                0,
                1,
                "Design patterns in practice",
                0.08
        );

        when(documentChunkRepository.searchSimilarChunks(
                eq(subjectId), eq(List.of(docId)), eq(fakeEmbedding), anyDouble(), anyInt()
        )).thenReturn(List.of(chunkResult));

        List<KnowledgeSearchResult> results = service.searchKnowledge(subjectId, "design pattern", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Design patterns in practice");
        assertThat(results.get(0).getSimilarity()).isEqualTo(0.92);
    }
}
