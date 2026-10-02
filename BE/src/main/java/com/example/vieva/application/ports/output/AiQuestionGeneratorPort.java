package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.BloomLevel;

import java.util.List;

public interface AiQuestionGeneratorPort {
    List<GeneratedQuestionItem> generateQuestions(
        String topicName,
        BloomLevel bloomLevel,
        int quantity,
        String customPrompt,
        List<ChunkSearchResult> contextChunks
    );
}
