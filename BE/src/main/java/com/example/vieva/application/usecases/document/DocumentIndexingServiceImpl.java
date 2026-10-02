package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentParserPort documentParser;
    private final FileStoragePort fileStorage;
    private final EmbeddingModelPort embeddingModel;
    private final TextSplitterPort textSplitter;
    private final DomainEventPublisherPort domainEventPublisher;

    @Override
    @Transactional
    public CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, String mimeType, UUID uploadedBy) {
        if (content == null || content.length == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 1. Upload to storage
        String fileUrl = fileStorage.uploadFile(filename, content, mimeType);

        // 2. Persist CourseDocument with status UPLOADED
        UUID documentId = UUID.randomUUID();
        CourseDocument doc = CourseDocument.builder()
                .documentId(documentId)
                .subjectId(subjectId)
                .uploadedBy(uploadedBy)
                .fileName(filename)
                .fileUrl(fileUrl)
                .fileSizeBytes((long) content.length)
                .mimeType(mimeType)
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .totalChunks(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CourseDocument saved = courseDocumentRepository.save(doc);

        // 3. Publish event to be processed AFTER_COMMIT
        domainEventPublisher.publishDocumentUploaded(documentId, content);

        return saved;
    }

    @Override
    @Transactional
    public void indexDocument(UUID documentId, byte[] fileBytes) {
        CourseDocument doc = courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        doc.setIndexingStatus(DocumentIndexingStatus.INDEXING);
        doc.setUpdatedAt(Instant.now());
        courseDocumentRepository.save(doc);

        try {
            // 1. Parse text with Tika
            String extractedText = documentParser.extractText(new ByteArrayInputStream(fileBytes));

            // Validate text layer (prevent scanned PDFs with no text)
            if (extractedText == null || extractedText.trim().length() < 100) {
                doc.setIndexingStatus(DocumentIndexingStatus.FAILED);
                doc.setErrorMessage(ErrorCode.EMPTY_DOCUMENT_TEXT.getMessage());
                doc.setUpdatedAt(Instant.now());
                courseDocumentRepository.save(doc);
                log.warn("Document {} text extraction failed: less than 100 characters", documentId);
                return;
            }

            // 2. Token-based chunking (framework splitter hidden behind TextSplitterPort)
            List<String> chunkTexts = textSplitter.split(extractedText);
            if (chunkTexts == null || chunkTexts.isEmpty()) {
                chunkTexts = List.of(extractedText);
            }

            // 3. Generate embeddings
            List<float[]> embeddings = embeddingModel.generateEmbeddings(chunkTexts);

            // 4. Save chunks
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkTexts.size(); i++) {
                String text = chunkTexts.get(i);
                float[] emb = (embeddings != null && i < embeddings.size()) ? embeddings.get(i) : new float[1536];

                DocumentChunk chunk = DocumentChunk.builder()
                        .chunkId(UUID.randomUUID())
                        .documentId(documentId)
                        .chunkIndex(i + 1)
                        .content(text)
                        .tokenCount(Math.max(1, text.length() / 4))
                        .embedding(emb)
                        .createdAt(Instant.now())
                        .build();
                chunks.add(chunk);
            }

            documentChunkRepository.saveAll(chunks);

            // 5. Update CourseDocument to READY
            doc.setIndexingStatus(DocumentIndexingStatus.READY);
            doc.setTotalChunks(chunks.size());
            doc.setExtractedTextSummary(extractedText.substring(0, Math.min(extractedText.length(), 500)));
            doc.setErrorMessage(null);
            doc.setUpdatedAt(Instant.now());
            courseDocumentRepository.save(doc);
            log.info("Successfully indexed document {} with {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Error indexing document {}", documentId, e);
            doc.setIndexingStatus(DocumentIndexingStatus.FAILED);
            doc.setErrorMessage("Indexing failed: " + e.getMessage());
            doc.setUpdatedAt(Instant.now());
            courseDocumentRepository.save(doc);
        }
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
    public void softDeleteDocument(UUID documentId, UUID currentUserId) {
        courseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        courseDocumentRepository.softDelete(documentId);
        log.info("User {} soft-deleted document {}", currentUserId, documentId);
    }

    @Override
    @Transactional
    public int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold) {
        List<CourseDocument> staleIndexing = courseDocumentRepository.findStaleIndexingDocuments(indexingThreshold);
        for (CourseDocument doc : staleIndexing) {
            log.warn("Marking stale indexing document {} as FAILED (stuck beyond threshold)", doc.getDocumentId());
            doc.markFailed("Indexing operation timed out after 30 minutes of inactivity");
        }

        // Also handle uploaded documents never picked up (e.g. server restarted before listener fired)
        List<CourseDocument> staleUploaded = courseDocumentRepository.findStaleUploadedDocuments(uploadedThreshold);
        for (CourseDocument doc : staleUploaded) {
            log.warn("Marking abandoned uploaded document {} as FAILED (no worker pickup)", doc.getDocumentId());
            doc.markFailed("Document upload processing was not picked up by worker");
        }

        List<CourseDocument> allStale = new ArrayList<>(staleIndexing);
        allStale.addAll(staleUploaded);
        courseDocumentRepository.saveAll(allStale);
        return allStale.size();
    }
}
