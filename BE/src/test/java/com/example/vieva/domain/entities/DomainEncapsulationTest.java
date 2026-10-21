package com.example.vieva.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEncapsulationTest {

    @Test
    @DisplayName("DocumentChunk getEmbedding returns a clone preventing mutation of internal state")
    void documentChunkEmbeddingEncapsulation() {
        float[] original = new float[]{1.0f, 2.0f, 3.0f};
        DocumentChunk chunk = DocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .embedding(original)
                .build();

        float[] retrieved = chunk.getEmbedding();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).containsExactly(1.0f, 2.0f, 3.0f);
        assertThat(retrieved).isNotSameAs(original);

        retrieved[0] = 99.0f;
        assertThat(chunk.getEmbedding()[0]).isEqualTo(1.0f);

        DocumentChunk nullChunk = new DocumentChunk();
        assertThat(nullChunk.getEmbedding()).isNull();
    }

    @Test
    @DisplayName("DocumentChunk setEmbedding performs defensive clone preventing mutation of caller array")
    void documentChunkSetEmbeddingEncapsulation() {
        float[] callerArray = new float[]{4.0f, 5.0f, 6.0f};
        DocumentChunk chunk = new DocumentChunk();
        chunk.setEmbedding(callerArray);

        callerArray[0] = 999.0f;
        assertThat(chunk.getEmbedding()[0]).isEqualTo(4.0f);

        chunk.setEmbedding(null);
        assertThat(chunk.getEmbedding()).isNull();
    }

    @Test
    @DisplayName("RubricCriterion getPerformanceLevels returns unmodifiable list and handles null")
    void rubricCriterionPerformanceLevelsEncapsulation() {
        PerformanceLevel level = new PerformanceLevel("A", "Good", new BigDecimal("5.0"));
        RubricCriterion criterion = RubricCriterion.builder()
                .criterionId(UUID.randomUUID())
                .performanceLevels(List.of(level))
                .build();

        List<PerformanceLevel> levels = criterion.getPerformanceLevels();
        assertThat(levels).containsExactly(level);
        assertThatThrownBy(() -> levels.add(new PerformanceLevel("B", "Fair", new BigDecimal("3.0"))))
                .isInstanceOf(UnsupportedOperationException.class);

        RubricCriterion emptyCriterion = RubricCriterion.builder().build();
        emptyCriterion.setPerformanceLevels(null);
        assertThat(emptyCriterion.getPerformanceLevels()).isEmpty();
    }

    @Test
    @DisplayName("RubricCriterion setPerformanceLevels performs defensive copy")
    void rubricCriterionSetPerformanceLevelsEncapsulation() {
        PerformanceLevel level = new PerformanceLevel("A", "Good", BigDecimal.ONE);
        List<PerformanceLevel> mutableList = new ArrayList<>();
        mutableList.add(level);

        RubricCriterion criterion = new RubricCriterion();
        criterion.setPerformanceLevels(mutableList);

        mutableList.clear();
        assertThat(criterion.getPerformanceLevels()).containsExactly(level);
    }

    @Test
    @DisplayName("QuestionGenerationRequest protects collection getters against external mutation")
    void questionGenerationRequestCollectionsEncapsulation() {
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .generationRequestId(UUID.randomUUID())
                .documentIds(List.of(docId))
                .retrievedChunkIds(List.of(chunkId))
                .issues(List.of("Issue 1"))
                .bloomDistribution(Map.of(BloomLevel.REMEMBER, 2))
                .build();

        List<UUID> docs = request.getDocumentIds();
        assertThat(docs).containsExactly(docId);
        assertThatThrownBy(() -> docs.add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);

        List<UUID> chunks = request.getRetrievedChunkIds();
        assertThat(chunks).containsExactly(chunkId);
        assertThatThrownBy(() -> chunks.add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);

        List<String> issues = request.getIssues();
        assertThat(issues).containsExactly("Issue 1");
        assertThatThrownBy(() -> issues.add("New issue"))
                .isInstanceOf(UnsupportedOperationException.class);

        Map<BloomLevel, Integer> bloom = request.getBloomDistribution();
        assertThat(bloom).containsEntry(BloomLevel.REMEMBER, 2);
        assertThatThrownBy(() -> bloom.put(BloomLevel.APPLY, 1))
                .isInstanceOf(UnsupportedOperationException.class);

        QuestionGenerationRequest nullCollectionsRequest = new QuestionGenerationRequest();
        nullCollectionsRequest.setDocumentIds(null);
        nullCollectionsRequest.setRetrievedChunkIds(null);
        nullCollectionsRequest.setIssues(null);
        nullCollectionsRequest.setBloomDistribution(null);

        assertThat(nullCollectionsRequest.getDocumentIds()).isEmpty();
        assertThat(nullCollectionsRequest.getRetrievedChunkIds()).isEmpty();
        assertThat(nullCollectionsRequest.getIssues()).isEmpty();
        assertThat(nullCollectionsRequest.getBloomDistribution()).isEmpty();
    }

    @Test
    @DisplayName("QuestionGenerationRequest setters defensively copy collections")
    void questionGenerationRequestSettersDefensiveCopy() {
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        List<UUID> docs = new ArrayList<>(List.of(docId));
        List<UUID> chunks = new ArrayList<>(List.of(chunkId));
        List<String> issues = new ArrayList<>(List.of("Issue 1"));
        Map<BloomLevel, Integer> bloom = new HashMap<>(Map.of(BloomLevel.APPLY, 1));

        QuestionGenerationRequest request = new QuestionGenerationRequest();
        request.setDocumentIds(docs);
        request.setRetrievedChunkIds(chunks);
        request.setIssues(issues);
        request.setBloomDistribution(bloom);

        docs.clear();
        chunks.clear();
        issues.clear();
        bloom.clear();

        assertThat(request.getDocumentIds()).containsExactly(docId);
        assertThat(request.getRetrievedChunkIds()).containsExactly(chunkId);
        assertThat(request.getIssues()).containsExactly("Issue 1");
        assertThat(request.getBloomDistribution()).containsEntry(BloomLevel.APPLY, 1);
    }

    @Test
    @DisplayName("QuestionGenerationRequest start handles null documentIds and null distribution safely")
    void questionGenerationRequestStartNullSafety() {
        BloomDistribution dist = BloomDistribution.single(BloomLevel.REMEMBER);
        QuestionGenerationRequest req = QuestionGenerationRequest.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, dist, "note");
        assertThat(req.getDocumentIds()).isEmpty();
        assertThat(req.getTotalQuestions()).isEqualTo(1);
        assertThat(req.getStatus()).isEqualTo(QuestionGenerationStatus.PENDING);

        QuestionGenerationRequest nullDistReq = QuestionGenerationRequest.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                List.of(UUID.randomUUID()), null, "note");
        assertThat(nullDistReq.getBloomDistribution()).isEmpty();
        assertThat(nullDistReq.getTotalQuestions()).isEqualTo(0);
    }

    @Test
    @DisplayName("QuestionGenerationRequest recordOutcome safely appends issues even when initialized with immutable list")
    void questionGenerationRequestRecordOutcomeWithImmutableIssues() {
        QuestionGenerationRequest req = QuestionGenerationRequest.builder()
                .totalQuestions(2)
                .issues(List.of("Seed issue"))
                .build();

        req.recordOutcome(1, 0, List.of("New issue 1"), null);
        assertThat(req.getIssues()).containsExactly("Seed issue", "New issue 1");
        assertThat(req.getStatus()).isEqualTo(QuestionGenerationStatus.PARTIAL);

        req.recordOutcome(1, 0, List.of("New issue 2"), null);
        assertThat(req.getIssues()).containsExactly("Seed issue", "New issue 1", "New issue 2");
        assertThat(req.getStatus()).isEqualTo(QuestionGenerationStatus.COMPLETED);
    }
}
