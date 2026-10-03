package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fixed JSON schema exchanged with the LLM (UC1.2):
 * <pre>
 * {"questions":[{"content":"...","expectedAnswer":"...","bloomLevel":"APPLY",
 *   "rubric":{"totalScore":10,"criteria":[{"name":"...","description":"...","maxScore":5}]},
 *   "sourceChunkIds":["C1"],"citations":[{"chunkId":"C1","quote":"verbatim text"}]}]}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmQuestionPayload(List<Question> questions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Question(String content, String expectedAnswer, String bloomLevel, Rubric rubric,
                           List<String> sourceChunkIds, List<Citation> citations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rubric(BigDecimal totalScore, List<Criterion> criteria) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Criterion(String name, String description, BigDecimal maxScore) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Citation(String chunkId, String quote) {
    }

    public List<GeneratedQuestionCandidate> toCandidates() {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .filter(java.util.Objects::nonNull)
                .map(q -> new GeneratedQuestionCandidate(
                        q.content(),
                        q.expectedAnswer(),
                        q.bloomLevel(),
                        q.rubric() == null ? null : q.rubric().totalScore(),
                        q.rubric() == null || q.rubric().criteria() == null ? List.of()
                                : q.rubric().criteria().stream()
                                .filter(java.util.Objects::nonNull)
                                .map(c -> new GeneratedQuestionCandidate.Criterion(c.name(), c.description(), c.maxScore()))
                                .toList(),
                        q.sourceChunkIds() == null ? List.of() : q.sourceChunkIds(),
                        q.citations() == null ? List.of()
                                : q.citations().stream()
                                .filter(java.util.Objects::nonNull)
                                .map(c -> new GeneratedQuestionCandidate.Citation(c.chunkId(), c.quote()))
                                .toList()))
                .toList();
    }
}
