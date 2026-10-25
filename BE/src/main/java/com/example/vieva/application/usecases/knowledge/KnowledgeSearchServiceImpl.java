package com.example.vieva.application.usecases.knowledge;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchResult;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchService;
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
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UseCaseTransaction
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingModelPort embeddingModel;
    private final SubjectAccessGuard subjectAccessGuard;
    private final TopicRepository topicRepository;
    private final TextSplitterPort textSplitter;
    private final FileStoragePort fileStoragePort;

    public KnowledgeSearchServiceImpl(
            CourseDocumentRepository courseDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingModelPort embeddingModel,
            SubjectAccessGuard subjectAccessGuard,
            TopicRepository topicRepository,
            TextSplitterPort textSplitter,
            FileStoragePort fileStoragePort
    ) {
        this.courseDocumentRepository = java.util.Objects.requireNonNull(courseDocumentRepository, "courseDocumentRepository");
        this.documentChunkRepository = java.util.Objects.requireNonNull(documentChunkRepository, "documentChunkRepository");
        this.embeddingModel = java.util.Objects.requireNonNull(embeddingModel, "embeddingModel");
        this.subjectAccessGuard = java.util.Objects.requireNonNull(subjectAccessGuard, "subjectAccessGuard");
        this.topicRepository = java.util.Objects.requireNonNull(topicRepository, "topicRepository");
        this.textSplitter = java.util.Objects.requireNonNull(textSplitter, "textSplitter");
        this.fileStoragePort = java.util.Objects.requireNonNull(fileStoragePort, "fileStoragePort");
    }

    @Override
    @UseCaseTransaction
    public KnowledgeSearchResult createKnowledge(UUID subjectId, String content, UUID topicId, UUID actorId) {
        if (subjectId == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Subject ID must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Content must not be blank");
        }
        if (content.length() > 50000) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Knowledge content exceeds maximum limit of 50000 characters");
        }
        subjectAccessGuard.requireManage(actorId, subjectId);

        if (topicId != null && !topicRepository.existsByIdAndSubjectId(topicId, subjectId)) {
            throw new ApplicationException(ErrorCode.TOPIC_NOT_FOUND, "Topic does not belong to subject");
        }

        UUID docId = UUID.randomUUID();
        String docName = "Knowledge-" + System.currentTimeMillis() + ".txt";
        byte[] bytes = content.trim().getBytes(StandardCharsets.UTF_8);

        StoredFile stored = fileStoragePort.store(docName, bytes, "text/plain");
        if (stored == null || stored.url() == null || stored.url().isBlank()) {
            throw new ApplicationException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Failed to persist knowledge text file");
        }

        try {
            List<TextChunk> textChunks = textSplitter.split(
                    List.of(new ParsedSection(content.trim(), 1, "Direct Knowledge"))
            );
            if (textChunks == null || textChunks.isEmpty()) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Knowledge content could not be split into chunks");
            }

            Instant now = Instant.now();
            List<DocumentChunk> chunks = new ArrayList<>(textChunks.size());
            String metadataJson = topicId != null ? "{\"topicId\":\"" + topicId + "\"}" : "{}";
            for (int i = 0; i < textChunks.size(); i++) {
                TextChunk piece = textChunks.get(i);
                float[] embedding = embeddingModel.generateEmbedding(piece.text());
                if (embedding == null || embedding.length != 1536) {
                    throw new ApplicationException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                            "Generated embedding is invalid or dimensions mismatch (expected 1536, got "
                                    + (embedding == null ? "null" : embedding.length) + ")");
                }
                chunks.add(DocumentChunk.builder()
                        .chunkId(UUID.randomUUID())
                        .documentId(docId)
                        .chunkIndex(i)
                        .content(piece.text())
                        .tokenCount(piece.tokenCount() > 0 ? piece.tokenCount() : piece.text().split("\\s+").length)
                        .embedding(embedding)
                        .metadataJson(metadataJson)
                        .createdAt(now)
                        .build());
            }

            CourseDocument doc = CourseDocument.builder()
                    .documentId(docId)
                    .subjectId(subjectId)
                    .topicId(topicId)
                    .uploadedBy(actorId)
                    .fileName(docName)
                    .fileUrl(stored.url())
                    .storageKey(stored.storageKey())
                    .fileSizeBytes((long) bytes.length)
                    .mimeType("text/plain")
                    .indexingStatus(DocumentStatus.READY)
                    .totalChunks(chunks.size())
                    .indexAttempts(1)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            courseDocumentRepository.save(doc);
            documentChunkRepository.saveAll(chunks);

            DocumentChunk firstChunk = chunks.get(0);
            return KnowledgeSearchResult.builder()
                    .chunkId(firstChunk.getChunkId())
                    .documentId(docId)
                    .documentName(docName)
                    .chunkIndex(0)
                    .pageStart(1)
                    .content(firstChunk.getContent())
                    .similarity(1.0)
                    .build();
        } catch (RuntimeException e) {
            if (stored.storageKey() != null) {
                fileStoragePort.delete(stored.storageKey());
            }
            throw e;
        }
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<KnowledgeSearchResult> listReadyChunks(UUID subjectId, int limit) {
        if (subjectId == null) {
            return List.of();
        }
        int targetLimit = limit > 0 ? Math.min(limit, 50) : 10;
        List<CourseDocument> readyDocs = courseDocumentRepository.findActiveBySubjectId(subjectId).stream()
                .filter(CourseDocument::isReady)
                .toList();

        List<KnowledgeSearchResult> results = new ArrayList<>();
        for (CourseDocument doc : readyDocs) {
            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(doc.getDocumentId());
            for (DocumentChunk chunk : chunks) {
                results.add(KnowledgeSearchResult.builder()
                        .chunkId(chunk.getChunkId())
                        .documentId(doc.getDocumentId())
                        .documentName(doc.getFileName())
                        .chunkIndex(chunk.getChunkIndex())
                        .pageStart(1)
                        .content(chunk.getContent())
                        .similarity(1.0)
                        .build());
                if (results.size() >= targetLimit) {
                    return results;
                }
            }
        }
        return results;
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<KnowledgeSearchResult> searchKnowledge(UUID subjectId, String query, int limit) {
        if (subjectId == null || query == null || query.isBlank()) {
            return List.of();
        }

        List<UUID> docIds = courseDocumentRepository.findActiveBySubjectId(subjectId).stream()
                .filter(CourseDocument::isReady)
                .map(CourseDocument::getDocumentId)
                .toList();

        if (docIds.isEmpty()) {
            return List.of();
        }

        int targetLimit = limit > 0 ? Math.min(limit, 50) : 10;
        float[] queryEmbedding = embeddingModel.generateEmbedding(query.trim());

        List<ChunkSearchResult> chunks = documentChunkRepository.searchSimilarChunks(
                subjectId, docIds, queryEmbedding, 0.0, targetLimit
        );

        return chunks.stream()
                .map(c -> KnowledgeSearchResult.builder()
                        .chunkId(c.chunkId())
                        .documentId(c.documentId())
                        .documentName(c.documentName())
                        .chunkIndex(c.chunkIndex())
                        .pageStart(c.page())
                        .content(c.content())
                        .similarity(c.similarityScore())
                        .build())
                .toList();
    }
}
