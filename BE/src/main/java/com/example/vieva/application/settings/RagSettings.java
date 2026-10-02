package com.example.vieva.application.settings;

import lombok.Data;

/**
 * UC1.1/UC1.2 tuning, bound from {@code vieva.rag.*}.
 */
@Data
public class RagSettings {
    /** Chunks retrieved per generation request. */
    private int topK = 8;
    /** Minimum cosine similarity for a chunk to be used as context. */
    private double minSimilarity = 0.2;
    private int maxQuestionsPerRequest = 20;
    /** LLM calls inside one generation run (to top up rejected candidates). */
    private int attemptsPerRun = 3;
    /** LLM calls allowed over the whole life of a generation request, retries included. */
    private int maxTotalAttempts = 9;
    /** Regenerations allowed per AI draft. */
    private int maxRegenerationsPerVersion = 5;
    /** Jaccard similarity above which a generated question counts as a duplicate. */
    private double duplicateThreshold = 0.8;
    /** Existing questions shown to the LLM as "do not repeat". */
    private int avoidListSize = 30;
    private int embeddingBatchSize = 64;
    /** Expected embedding size; must match the vector(1536) column. */
    private int embeddingDimensions = 1536;
}
