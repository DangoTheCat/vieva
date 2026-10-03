package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuestionGenerationRequestPersistenceMapper {

    private static final TypeReference<List<UUID>> UUID_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<BloomLevel, Integer>> BLOOM_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public QuestionGenerationRequest toDomain(QuestionGenerationRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<BloomLevel, Integer> distribution = new EnumMap<>(BloomLevel.class);
        distribution.putAll(read(entity.getBloomDistribution(), BLOOM_MAP, Map.of()));
        return QuestionGenerationRequest.builder()
                .generationRequestId(entity.getGenerationRequestId())
                .subjectId(entity.getSubjectId())
                .topicId(entity.getTopicId())
                .requestedBy(entity.getRequestedBy())
                .documentIds(new ArrayList<>(read(entity.getDocumentIds(), UUID_LIST, List.of())))
                .bloomDistribution(distribution)
                .totalQuestions(entity.getTotalQuestions())
                .lecturerNote(entity.getLecturerNote())
                .retrievedChunkIds(new ArrayList<>(read(entity.getRetrievedChunkIds(), UUID_LIST, List.of())))
                .status(entity.getStatus())
                .generatedCount(entity.getGeneratedCount())
                .rejectedCount(entity.getRejectedCount())
                .attemptCount(entity.getAttemptCount())
                .issues(new ArrayList<>(read(entity.getIssuesJson(), STRING_LIST, List.of())))
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public QuestionGenerationRequestJpaEntity toEntity(QuestionGenerationRequest domain) {
        if (domain == null) {
            return null;
        }
        return QuestionGenerationRequestJpaEntity.builder()
                .generationRequestId(domain.getGenerationRequestId())
                .subjectId(domain.getSubjectId())
                .topicId(domain.getTopicId())
                .requestedBy(domain.getRequestedBy())
                .documentIds(write(domain.getDocumentIds()))
                .bloomDistribution(write(domain.getBloomDistribution()))
                .totalQuestions(domain.getTotalQuestions())
                .lecturerNote(domain.getLecturerNote())
                .retrievedChunkIds(write(domain.getRetrievedChunkIds()))
                .status(domain.getStatus())
                .generatedCount(domain.getGeneratedCount())
                .rejectedCount(domain.getRejectedCount())
                .attemptCount(domain.getAttemptCount())
                .issuesJson(write(domain.getIssues()))
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }

    private <T> T read(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted JSON column on question_generation_requests", e);
        }
    }

    private String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize question generation request", e);
        }
    }
}
