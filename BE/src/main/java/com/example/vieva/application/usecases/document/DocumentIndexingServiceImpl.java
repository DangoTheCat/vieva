package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.FileTypeDetectorPort;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.StoredFile;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC1.1. Indexing keeps slow work (storage read, parsing, embedding calls) outside of database
 * transactions: status changes and chunk writes run in short transactions of their own, so the
 * lecturer can observe UPLOADED → INDEXING → READY | FAILED while it runs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private static final int SUMMARY_LENGTH = 500;

    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final SubjectRepository subjectRepository;
    private final DocumentParserPort documentParser;
    private final FileStoragePort fileStorage;
    private final FileTypeDetectorPort fileTypeDetector;
    private final EmbeddingModelPort embeddingModel;
    private final TextSplitterPort textSplitter;
    private final DomainEventPublisherPort domainEventPublisher;
    private final TransactionRunnerPort transactionRunner;
    private final JsonSerializerPort jsonSerializer;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionBankAuditor auditor;
    private final DocumentSettings documentSettings;
    private final RagSettings ragSettings;

    @Override
    @Transactional
    public CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId) {
        subjectAccessGuard.requireManage(actorId, subjectId);
        subjectRepository.findById(subjectId)
                .filter(subject -> subject.getStatus() == SubjectStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_INACTIVE));

        DocumentFileValidator.validateSize(content, documentSettings);
        String safeName = DocumentFileValidator.sanitizeFilename(filename);
        String mimeType = DocumentFileValidator.validateType(safeName,
                fileTypeDetector.detect(content, safeName), documentSettings);

        StoredFile stored = fileStorage.store(safeName, content, mimeType);
        Instant now = Instant.now();
        CourseDocument document = courseDocumentRepository.save(CourseDocument.builder()
                .documentId(UUID.randomUUID())
                .subjectId(subjectId)
                .uploadedBy(actorId)
                .fileName(safeName)
                .fileUrl(stored.url())
                .storageKey(stored.storageKey())
                .fileSizeBytes((long) content.length)
                .mimeType(mimeType)
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .totalChunks(0)
                .indexAttempts(0)
                .createdAt(now)
                .updatedAt(now)
                .build());

        auditor.record(actorId, "DOCUMENT_UPLOADED", QuestionBankAuditor.DOCUMENT, document.getDocumentId(),
                Map.of("subjectId", subjectId, "fileName", safeName, "sizeBytes", content.length, "mimeType", mimeType));
        domainEventPublisher.publishDocumentIndexingRequested(document.getDocumentId());
        return document;
    }

    @Override
    public void indexDocument(UUID documentId) {
        CourseDocument document = transactionRunner.inNewTransaction(() -> {
            CourseDocument doc = courseDocumentRepository.findById(documentId).orElse(null);
            if (doc == null || doc.getIndexingStatus() != DocumentIndexingStatus.UPLOADED) {
                return null;
            }
            doc.startIndexing();
            return courseDocumentRepository.save(doc);
        });
        if (document == null) {
            log.info("Skip indexing of document {}: missing, deleted or not UPLOADED", documentId);
            return;
        }

        try {
            byte[] bytes = fileStorage.load(document.getStorageKey());
            List<ParsedSection> sections = documentParser.extract(bytes, document.getMimeType());
            String fullText = joinText(sections);
            if (fullText.replaceAll("\\s+", "").length() < documentSettings.getMinTextChars()) {
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT);
            }

            List<TextChunk> pieces = textSplitter.split(sections);
            if (pieces.isEmpty()) {
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT);
            }
            List<float[]> embeddings = embedInBatches(pieces);
            List<DocumentChunk> chunks = toChunks(documentId, pieces, embeddings);
            String summary = fullText.substring(0, Math.min(fullText.length(), SUMMARY_LENGTH));

            transactionRunner.inNewTransaction(() -> {
                CourseDocument doc = courseDocumentRepository.findById(documentId)
                        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
                // Clean slate: a previous partial run never leaves chunks behind.
                documentChunkRepository.deleteByDocumentId(documentId);
                documentChunkRepository.saveAll(chunks);
                doc.markReady(chunks.size(), summary);
                courseDocumentRepository.save(doc);
                auditor.record(null, "DOCUMENT_INDEXED", QuestionBankAuditor.DOCUMENT, documentId,
                        Map.of("chunks", chunks.size(), "attempt", doc.getIndexAttempts(),
                                "uploadedBy", String.valueOf(doc.getUploadedBy())));
            });
            log.info("Indexed document {} into {} chunks", documentId, chunks.size());
        } catch (Exception e) {
            String reason = failureReason(e);
            log.warn("Indexing of document {} failed: {}", documentId, reason);
            markIndexingFailed(documentId, reason);
        }
    }

    @Override
    @Transactional
    public CourseDocument retryIndexing(UUID documentId, UUID actorId) {
        CourseDocument document = courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId, document.getSubjectId());
        document.requeueForRetry(documentSettings.getMaxIndexAttempts());
        documentChunkRepository.deleteByDocumentId(documentId);
        CourseDocument saved = courseDocumentRepository.save(document);
        auditor.record(actorId, "DOCUMENT_REINDEX_REQUESTED", QuestionBankAuditor.DOCUMENT, documentId,
                Map.of("attemptsSoFar", saved.getIndexAttempts()));
        domainEventPublisher.publishDocumentIndexingRequested(documentId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDocument> getDocumentsBySubject(UUID subjectId) {
        return courseDocumentRepository.findActiveBySubjectId(subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDocument getDocumentById(UUID documentId) {
        return courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Override
    @Transactional
    public void softDeleteDocument(UUID documentId, UUID actorId) {
        CourseDocument document = courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId, document.getSubjectId());
        // Chunks of this document are evidence for questions (BR-03/BR-04).
        if (questionSourceRepository.existsByDocumentId(documentId)) {
            throw new AppException(ErrorCode.DOCUMENT_IN_USE);
        }
        document.softDelete();
        courseDocumentRepository.save(document);
        auditor.record(actorId, "DOCUMENT_DELETED", QuestionBankAuditor.DOCUMENT, documentId,
                Map.of("fileName", document.getFileName()));
    }

    @Override
    @Transactional
    public int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold) {
        List<CourseDocument> stale = new ArrayList<>();
        for (CourseDocument doc : courseDocumentRepository.findStaleIndexingDocuments(indexingThreshold)) {
            doc.markFailed("Indexing timed out; retry the document");
            stale.add(doc);
        }
        for (CourseDocument doc : courseDocumentRepository.findStaleUploadedDocuments(uploadedThreshold)) {
            doc.markFailed("Indexing was never picked up by a worker; retry the document");
            stale.add(doc);
        }
        for (CourseDocument doc : stale) {
            documentChunkRepository.deleteByDocumentId(doc.getDocumentId());
            auditor.record(null, "DOCUMENT_INDEX_FAILED", QuestionBankAuditor.DOCUMENT, doc.getDocumentId(),
                    Map.of("reason", doc.getErrorMessage()));
        }
        courseDocumentRepository.saveAll(stale);
        return stale.size();
    }

    private void markIndexingFailed(UUID documentId, String reason) {
        try {
            transactionRunner.inNewTransaction(() -> {
                courseDocumentRepository.findById(documentId).ifPresent(doc -> {
                    if (doc.getIndexingStatus() == DocumentIndexingStatus.READY) {
                        return;
                    }
                    documentChunkRepository.deleteByDocumentId(documentId);
                    doc.markFailed(reason);
                    courseDocumentRepository.save(doc);
                    auditor.record(null, "DOCUMENT_INDEX_FAILED", QuestionBankAuditor.DOCUMENT, documentId,
                            Map.of("reason", reason, "attempt", doc.getIndexAttempts()));
                });
            });
        } catch (Exception e) {
            // The stale-document scheduler will eventually move it to FAILED.
            log.error("Could not record indexing failure for document {}", documentId, e);
        }
    }

    private List<float[]> embedInBatches(List<TextChunk> pieces) {
        int batchSize = Math.max(1, ragSettings.getEmbeddingBatchSize());
        List<float[]> vectors = new ArrayList<>(pieces.size());
        for (int start = 0; start < pieces.size(); start += batchSize) {
            List<String> batch = pieces.subList(start, Math.min(start + batchSize, pieces.size())).stream()
                    .map(TextChunk::text)
                    .toList();
            List<float[]> result = embeddingModel.generateEmbeddings(batch);
            if (result == null || result.size() != batch.size()) {
                throw new AiServiceException("Embedding service returned " + (result == null ? 0 : result.size())
                        + " vectors for " + batch.size() + " chunks");
            }
            for (float[] vector : result) {
                if (vector == null || vector.length != ragSettings.getEmbeddingDimensions()) {
                    throw new AiServiceException("Embedding has " + (vector == null ? 0 : vector.length)
                            + " dimensions, expected " + ragSettings.getEmbeddingDimensions());
                }
            }
            vectors.addAll(result);
        }
        return vectors;
    }

    private List<DocumentChunk> toChunks(UUID documentId, List<TextChunk> pieces, List<float[]> embeddings) {
        List<DocumentChunk> chunks = new ArrayList<>(pieces.size());
        Instant now = Instant.now();
        for (int i = 0; i < pieces.size(); i++) {
            TextChunk piece = pieces.get(i);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageStart", piece.pageStart());
            metadata.put("pageEnd", piece.pageEnd());
            metadata.put("section", piece.sectionTitle());
            chunks.add(DocumentChunk.builder()
                    .chunkId(UUID.randomUUID())
                    .documentId(documentId)
                    .chunkIndex(i + 1)
                    .content(piece.text())
                    .tokenCount(piece.tokenCount())
                    .embedding(embeddings.get(i))
                    .metadataJson(jsonSerializer.serialize(metadata))
                    .createdAt(now)
                    .build());
        }
        return chunks;
    }

    private static String joinText(List<ParsedSection> sections) {
        StringBuilder builder = new StringBuilder();
        for (ParsedSection section : sections) {
            if (section.text() != null && !section.text().isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append(section.text().trim());
            }
        }
        return builder.toString();
    }

    /** User-facing reason; never includes stack traces or provider secrets. */
    private static String failureReason(Exception e) {
        if (e instanceof AppException app) {
            return app.getMessage();
        }
        if (e instanceof AiServiceException) {
            return "Embedding service error: " + e.getMessage();
        }
        return "Text extraction or indexing failed (" + e.getClass().getSimpleName() + ")";
    }
}
