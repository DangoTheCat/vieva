package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.chat.request.*;
import dev.langchain4j.model.chat.request.json.*;
import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Fixed JSON schema exchanged with the LLM (UC1.2):
 * <pre>
 * {"questions":[{"content":"...","expectedAnswer":"...","bloomLevel":"APPLY",
 *   "rubric":{"totalScore":10,"criteria":[{"name":"...","description":"...","maxScore":5}]},
 *   "evidenceIds":["C1:E1"]}]}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmQuestionPayload(List<Question> questions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Question(String content, String expectedAnswer, String bloomLevel, Rubric rubric,
                           List<String> evidenceIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rubric(BigDecimal totalScore, List<Criterion> criteria) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Criterion(String name, String description, BigDecimal maxScore) {
    }

    public List<GeneratedQuestionCandidate> toCandidates(Map<String,GeneratedQuestionCandidate.Citation> evidence) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .filter(java.util.Objects::nonNull)
                .map(q -> {
                    var citations=q.evidenceIds()==null ? List.<GeneratedQuestionCandidate.Citation>of()
                            : q.evidenceIds().stream().distinct().map(id -> {
                                var citation=evidence.get(id);
                                if(citation==null) throw new AiServiceException("LLM selected an unknown evidence ID");
                                return citation;
                            }).toList();
                    return new GeneratedQuestionCandidate(
                        q.content(),
                        q.expectedAnswer(),
                        q.bloomLevel(),
                        q.rubric() == null ? null : q.rubric().totalScore(),
                        q.rubric() == null || q.rubric().criteria() == null ? List.of()
                                : q.rubric().criteria().stream()
                                .filter(java.util.Objects::nonNull)
                                .map(c -> new GeneratedQuestionCandidate.Criterion(c.name(), c.description(), c.maxScore()))
                                .toList(),
                        citations.stream().map(GeneratedQuestionCandidate.Citation::chunkRef).distinct().toList(),citations);
                })
                .toList();
    }

    static ResponseFormat responseFormat(QuestionGenerationPrompt prompt,List<String> evidenceIds) {
        var criterion=JsonObjectSchema.builder().addStringProperty("name").addStringProperty("description")
                .addNumberProperty("maxScore").required("name","description","maxScore").additionalProperties(false).build();
        var rubric=JsonObjectSchema.builder().addNumberProperty("totalScore")
                .addProperty("criteria",JsonArraySchema.builder().items(criterion).build())
                .required("totalScore","criteria").additionalProperties(false).build();
        var question=JsonObjectSchema.builder().addStringProperty("content").addStringProperty("expectedAnswer")
                .addEnumProperty("bloomLevel",prompt.bloomDistribution().keySet().stream().map(Enum::name).toList())
                .addProperty("rubric",rubric).addProperty("evidenceIds",JsonArraySchema.builder()
                        .items(JsonEnumSchema.builder().enumValues(evidenceIds).build()).build())
                .required("content","expectedAnswer","bloomLevel","rubric","evidenceIds").additionalProperties(false).build();
        return ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(JsonSchema.builder().name("generated_questions")
                .rootElement(JsonObjectSchema.builder().addProperty("questions",JsonArraySchema.builder().items(question).build())
                        .required("questions").additionalProperties(false).build()).build()).build();
    }
}
