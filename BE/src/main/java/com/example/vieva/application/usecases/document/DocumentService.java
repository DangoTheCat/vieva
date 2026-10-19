package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
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
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentStatus;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.vieva.application.transaction.UseCaseTransaction;

import com.example.vieva.application.ports.output.DocumentPreview;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
@RequiredArgsConstructor
public class DocumentService {
    @UseCaseTransaction
    public void archiveDocument(UUID documentId, UUID actorId) {
        CourseDocument document = courseDocumentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId,document.getSubjectId());
        document.archive();
        courseDocumentRepository.save(document);
    }

    @UseCaseTransaction
    public CourseDocument processDocumentUpload(UUID subjectId, String filename, byte[] content, UUID actorId) {
        return uploadDocument(subjectId,filename,content,actorId);
    }

    private static final int SUMMARY_LENGTH = 500;

    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final SubjectRepository subjectRepository;
    private final DocumentParserPort documentParser;
    private final FileStoragePort fileStorage;
    private final FileTypeDetectorPort fileTypeDetector;
    private final TextSplitterPort textSplitter;
    private final DomainEventPublisherPort domainEventPublisher;
    private final TransactionRunnerPort transactionRunner;
    private final JsonSerializerPort jsonSerializer;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionBankAuditor auditor;
    private final DocumentSettings documentSettings;
    private final com.example.vieva.application.ports.output.QuestionDraftWriter db;
    @UseCaseTransaction
    public CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId) {
        subjectAccessGuard.requireManage(actorId, subjectId);
        subjectRepository.findById(subjectId)
                .filter(subject -> subject.getStatus() == SubjectStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_INACTIVE));

        DocumentFileValidator.validateSize(content, documentSettings);
        String safeName = DocumentFileValidator.sanitizeFilename(filename);
        String mimeType = DocumentFileValidator.validateType(safeName,
                fileTypeDetector.detect(content, safeName), documentSettings);

        // Duplicate check within subject (same filename and file size)
        boolean isDuplicate = courseDocumentRepository.findActiveBySubjectId(subjectId).stream()
                .anyMatch(doc -> !doc.isDeleted()
                        && doc.getFileSizeBytes() != null
                        && doc.getFileSizeBytes().equals((long) content.length)
                        && doc.getFileName().equalsIgnoreCase(safeName));
        if (isDuplicate) {
            throw new AppException(ErrorCode.DOCUMENT_ALREADY_EXISTS, "Tệp tài liệu này (\"" + safeName + "\") đã được tải lên trước đó trong môn học này.");
        }

        StoredFile stored = fileStorage.store(safeName, content, mimeType);
        Instant now = Instant.now();
        CourseDocument document = db.saveCourseDocument(CourseDocument.builder()
                .documentId(UUID.randomUUID())
                .subjectId(subjectId)
                .uploadedBy(actorId)
                .fileName(safeName)
                .fileUrl(stored.url())
                .storageKey(stored.storageKey())
                .fileSizeBytes((long) content.length)
                .mimeType(mimeType)
                .indexingStatus(DocumentStatus.UPLOADED)
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
    public void indexDocument(UUID documentId) {
        CourseDocument document = transactionRunner.inNewTransaction(() -> {
            // Row write-lock: a concurrent worker/scheduler retry blocks here instead of
            // both reading UPLOADED and indexing twice (Rule 5).
            CourseDocument doc = courseDocumentRepository.findByIdForUpdate(documentId).orElse(null);
            if (doc == null || doc.getIndexingStatus() != DocumentStatus.UPLOADED) {
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
            int totalChars = fullText.replaceAll("\\s+", "").length();
            if (totalChars < documentSettings.getMinTextChars()) {
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT);
            }

            // Quality check for scanned / unextractable PDFs
            String ext = "";
            int dot = document.getFileName().lastIndexOf('.');
            if (dot > 0) ext = document.getFileName().substring(dot + 1).toLowerCase();
            int pageCount = (int) sections.stream()
                    .filter(s -> s.pageNumber() != null)
                    .map(ParsedSection::pageNumber)
                    .distinct()
                    .count();
            if (pageCount == 0) pageCount = 1;

            var problem = com.example.vieva.domain.services.ExtractionCheck.checkProblem(ext, pageCount, totalChars, sections.size());
            if (problem.isPresent()) {
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT, problem.get());
            }

            List<TextChunk> pieces = textSplitter.split(sections);
            if (pieces.isEmpty()) {
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT);
            }
            // Embed chunks with document context prefix for enhanced retrieval accuracy
            List<String> textsToEmbed = pieces.stream()
                    .map(p -> document.getFileName() + "\n" + p.text())
                    .toList();
            List<float[]> embeddings = documentChunkRepository.embedChunks(textsToEmbed);
            List<DocumentChunk> chunks = toChunks(documentId, pieces, embeddings);
            String summary = fullText.substring(0, Math.min(fullText.length(), SUMMARY_LENGTH));

            transactionRunner.inNewTransaction(() -> {
                CourseDocument doc = courseDocumentRepository.findById(documentId)
                        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
                // Clean slate: a previous partial run never leaves chunks behind.
                documentChunkRepository.deleteByDocumentId(documentId);
                documentChunkRepository.storeEmbeddings(chunks);
                db.updateDocumentStatus(doc,DocumentStatus.READY,chunks.size(),summary);
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
    @UseCaseTransaction
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
    @UseCaseTransaction(readOnly = true)
    public List<CourseDocument> getDocumentsBySubject(UUID subjectId) {
        return courseDocumentRepository.findActiveBySubjectId(subjectId);
    }
    @UseCaseTransaction(readOnly = true)
    public CourseDocument getDocumentById(UUID documentId) {
        return courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @UseCaseTransaction(readOnly = true)
    public DocumentPreview previewDocument(UUID documentId, UUID actorId) {
        CourseDocument document = courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId, document.getSubjectId());

        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(documentId);
        List<DocumentChunk> sortedChunks = new ArrayList<>(chunks);
        sortedChunks.sort(Comparator.comparing(DocumentChunk::getChunkIndex, Comparator.nullsLast(Comparator.naturalOrder())));

        List<DocumentPreview.PreviewChunk> previewChunks = sortedChunks.stream()
                .limit(5)
                .map(chunk -> {
                    String text = chunk.getContent() != null ? chunk.getContent().trim() : "";
                    boolean truncated = text.length() > 300;
                    String snippet = truncated ? text.substring(0, 300) + "..." : text;
                    String label = "Đoạn " + (chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 1);
                    if (chunk.getMetadataJson() != null && !chunk.getMetadataJson().isBlank()) {
                        try {
                            Map<?, ?> meta = jsonSerializer.deserialize(chunk.getMetadataJson(), Map.class);
                            if (meta != null && meta.containsKey("sourceLabel")) {
                                label = String.valueOf(meta.get("sourceLabel"));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return new DocumentPreview.PreviewChunk(label, snippet, truncated);
                })
                .toList();

        return new DocumentPreview(document, previewChunks);
    }
    @UseCaseTransaction
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
    @UseCaseTransaction
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
        List<UUID> staleIds = new ArrayList<>(stale.size());
        for (CourseDocument doc : stale) {
            staleIds.add(doc.getDocumentId());
            auditor.record(null, "DOCUMENT_INDEX_FAILED", QuestionBankAuditor.DOCUMENT, doc.getDocumentId(),
                    Map.of("reason", doc.getErrorMessage()));
        }
        // Re-read statuses: skip documents that concurrently reached READY (or were deleted)
        // after our read, so saveAll cannot overwrite fresh progress (Rule 5).
        List<CourseDocument> fresh = courseDocumentRepository.findAllByIds(staleIds);
        List<CourseDocument> toFail = new ArrayList<>(stale.size());
        for (CourseDocument doc : stale) {
            boolean stillStale = false;
            for (CourseDocument current : fresh) {
                if (current.getDocumentId().equals(doc.getDocumentId())
                        && current.getIndexingStatus() != DocumentStatus.READY
                        && current.getIndexingStatus() != DocumentStatus.ARCHIVED) {
                    stillStale = true;
                    break;
                }
            }
            if (stillStale) {
                toFail.add(doc);
            } else {
                log.info("Skip failing stale document {}: concurrently progressed or deleted", doc.getDocumentId());
            }
        }
        // One batched DELETE instead of one per stale document (Rule 4).
        List<UUID> toFailIds = new ArrayList<>(toFail.size());
        for (CourseDocument doc : toFail) {
            toFailIds.add(doc.getDocumentId());
        }
        documentChunkRepository.deleteByDocumentIds(toFailIds);
        courseDocumentRepository.saveAll(toFail);
        return toFail.size();
    }

    private void markIndexingFailed(UUID documentId, String reason) {
        try {
            transactionRunner.inNewTransaction(() -> {
                courseDocumentRepository.findById(documentId).ifPresent(doc -> {
                    if (doc.getIndexingStatus() == DocumentStatus.READY || doc.getIndexingStatus() == DocumentStatus.ARCHIVED) {
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

    private List<DocumentChunk> toChunks(UUID documentId, List<TextChunk> pieces, List<float[]> embeddings) {
        List<DocumentChunk> chunks = new ArrayList<>(pieces.size());
        Instant now = Instant.now();
        for (int i = 0; i < pieces.size(); i++) {
            TextChunk piece = pieces.get(i);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageStart", piece.pageStart());
            metadata.put("pageEnd", piece.pageEnd());
            metadata.put("section", piece.sectionTitle());

            String sourceLabel;
            if (piece.sectionTitle() != null && piece.sectionTitle().toLowerCase().contains("slide")) {
                sourceLabel = piece.sectionTitle();
            } else if (piece.pageStart() != null && piece.pageStart() > 0) {
                if (piece.pageEnd() != null && !piece.pageEnd().equals(piece.pageStart())) {
                    sourceLabel = "Trang " + piece.pageStart() + " - " + piece.pageEnd();
                } else {
                    sourceLabel = "Trang " + piece.pageStart();
                }
            } else {
                sourceLabel = "Đoạn " + (i + 1);
            }
            metadata.put("sourceLabel", sourceLabel);

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
