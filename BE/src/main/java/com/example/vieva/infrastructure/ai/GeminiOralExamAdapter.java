package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.EvaluationTurn;
import com.example.vieva.application.ports.output.RubricSummary;
import com.example.vieva.application.ports.output.VivaEvaluationContext;
import com.example.vieva.application.ports.output.VivaEvaluationGateway;
import com.example.vieva.application.ports.output.VivaEvaluationResult;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.ScoringMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.FinishReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * Gemini Oral Exam Adapter implementing VivaEvaluationGateway and AttemptGradingGateway.
 * Interacts with Gemini models (e.g. gemini-1.5-flash / gemini-2.0-flash-lite)
 * via LangChain4j ChatModel, prompting in Vietnamese and demanding structured JSON output.
 * If Gemini fails or times out, seamlessly falls back to RuleBasedGraderFallback.
 */
@Slf4j
@Service
@Primary
public class GeminiOralExamAdapter implements VivaEvaluationGateway, AttemptGradingGateway {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final RuleBasedGraderFallback fallbackGrader;

    public GeminiOralExamAdapter(ChatModel chatModel,
                                 ObjectMapper objectMapper,
                                 RuleBasedGraderFallback fallbackGrader) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.fallbackGrader = fallbackGrader;
    }

    public record TurnEvalLlmResponse(
            @JsonProperty("isStandardMet") boolean isStandardMet,
            @JsonProperty("shouldFollowUp") boolean shouldFollowUp,
            @JsonProperty("followUpQuestionText") String followUpQuestionText,
            @JsonProperty("score") Double score,
            @JsonProperty("feedback") String feedback,
            @JsonProperty("rubricEvaluations") Object rubricEvaluations
    ) {}

    public record GradeAttemptLlmResponse(
            @JsonProperty("totalScore") Double totalScore,
            @JsonProperty("gradeLevel") String gradeLevel,
            @JsonProperty("detailedFeedback") String detailedFeedback
    ) {}

    @Override
    public VivaEvaluationResult evaluateTurn(EvaluationTurn turn, RubricSummary rubric, VivaEvaluationContext context) {
        if (turn.transcriptText() == null || turn.transcriptText().trim().isEmpty()) {
            return fallbackGrader.evaluateTurn(turn, rubric, context);
        }

        try {
            String systemPrompt = """
                Bạn là một giám khảo vấn đáp học thuật nghiêm túc, công bằng và thấu hiểu.
                Nhiệm vụ của bạn là đánh giá câu trả lời của sinh viên cho một câu hỏi thi vấn đáp.
                Bạn PHẢI trả lời bằng tiếng Việt và xuất kết quả đúng định dạng JSON chuẩn với các trường sau:
                {
                  "isStandardMet": true/false (sinh viên đã trả lời đạt chuẩn kiến thức tối thiểu chưa),
                  "shouldFollowUp": true/false (có cần hỏi thêm câu hỏi phụ để làm rõ kiến thức không),
                  "followUpQuestionText": "câu hỏi phụ nếu shouldFollowUp=true, nếu không thì để rỗng hoặc null",
                  "score": số thực thang điểm 10 (ví dụ: 8.5),
                  "feedback": "nhận xét chi tiết bằng tiếng Việt về ưu điểm và thiếu sót của câu trả lời",
                  "rubricEvaluations": "chuỗi JSON chi tiết tiêu chí đã đạt"
                }
                Lưu ý:
                - Nếu sinh viên trả lời thiếu ý trọng tâm, hãy đặt isStandardMet=false, shouldFollowUp=true và cung cấp followUpQuestionText đào sâu vào điểm còn thiếu.
                - Nếu sinh viên trả lời xuất sắc hoặc đầy đủ, hãy đặt shouldFollowUp=false, followUpQuestionText=null.
                """;

            StringBuilder userMessageBuilder = new StringBuilder();
            userMessageBuilder.append("CÂU HỎI THI: ").append(turn.questionText()).append("\n\n");
            userMessageBuilder.append("CÂU TRẢ LỜI CỦA SINH VIÊN: ").append(turn.transcriptText()).append("\n\n");

            if (context != null && context.expectedAnswer() != null && !context.expectedAnswer().isBlank()) {
                userMessageBuilder.append("ĐÁP ÁN KỲ VỌNG / GỢI Ý ĐÁNH GIÁ:\n").append(context.expectedAnswer()).append("\n\n");
            }

            if (context != null && context.rubricJson() != null && !context.rubricJson().isBlank() && !context.rubricJson().equals("[]")) {
                userMessageBuilder.append("TIÊU CHÍ RUBRIC (FROZEN SNAPSHOT):\n").append(context.rubricJson()).append("\n\n");
            } else if (rubric != null) {
                userMessageBuilder.append("THÔNG TIN RUBRIC ĐÁNH GIÁ:\n");
                userMessageBuilder.append("- Tên Rubric: ").append(rubric.name()).append("\n");
                if (rubric.description() != null) {
                    userMessageBuilder.append("- Mô tả: ").append(rubric.description()).append("\n\n");
                }
            }

            if (context != null && context.ragContext() != null && !context.ragContext().isBlank()) {
                userMessageBuilder.append("TÀI LIỆU THAM KHẢO (SCOPED RAG):\n").append(context.ragContext()).append("\n\n");
            }

            var response = chatModel.chat(ChatRequest.builder().messages(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userMessageBuilder.toString()))
                    .build());

            String raw = response != null && response.aiMessage() != null ? response.aiMessage().text() : null;
            if (raw == null || raw.isBlank()) {
                throw new AiServiceException("Gemini returned empty evaluation response");
            }

            TurnEvalLlmResponse llmPayload = objectMapper.readValue(extractJson(raw), TurnEvalLlmResponse.class);

            Double rawScore = llmPayload.score();
            BigDecimal score;
            if (rawScore == null || !Double.isFinite(rawScore)) {
                score = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                double clamped = Math.max(0.0, Math.min(10.0, rawScore));
                score = BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
            }

            String rubricEvalStr = llmPayload.rubricEvaluations() != null
                    ? objectMapper.writeValueAsString(llmPayload.rubricEvaluations())
                    : "{}";

            return new VivaEvaluationResult(
                    llmPayload.isStandardMet(),
                    llmPayload.shouldFollowUp(),
                    llmPayload.followUpQuestionText(),
                    score,
                    llmPayload.feedback() != null ? llmPayload.feedback() : "",
                    rubricEvalStr
            );

        } catch (Exception ex) {
            log.warn("Gemini oral exam turn evaluation failed: {}, using RuleBasedGraderFallback", ex.getMessage());
            return fallbackGrader.evaluateTurn(turn, rubric, context);
        }
    }

    @Override
    public AttemptGradingResult gradeAttempt(List<EvaluationTurn> turns, RubricSummary rubric) {
        if (turns == null || turns.isEmpty()) {
            return fallbackGrader.gradeAttempt(turns, rubric);
        }

        try {
            String systemPrompt = """
                Bạn là Hội đồng chấm thi vấn đáp học thuật cao cấp.
                Nhiệm vụ của bạn là tổng hợp toàn bộ các lượt hỏi và trả lời của sinh viên trong suốt buổi thi,
                đối chiếu với yêu cầu học phần và thang điểm để tính điểm tổng kết và đưa ra đánh giá toàn diện.
                Bạn PHẢI trả lời bằng tiếng Việt và xuất định dạng JSON duy nhất như sau:
                {
                  "totalScore": số thực thang điểm 10 (ví dụ 8.25),
                  "gradeLevel": "EXCELLENT" | "GOOD" | "AVERAGE" | "POOR",
                  "detailedFeedback": "bản nhận xét chi tiết toàn diện bài thi vấn đáp của sinh viên"
                }
                Quy chuẩn gradeLevel:
                - EXCELLENT: điểm >= 8.5
                - GOOD: 7.0 <= điểm < 8.5
                - AVERAGE: 5.0 <= điểm < 7.0
                - POOR: điểm < 5.0
                """;

            StringBuilder userMessageBuilder = new StringBuilder();
            userMessageBuilder.append("CHI TIẾT CÁC LƯỢT THI VẤN ĐÁP:\n");
            for (int i = 0; i < turns.size(); i++) {
                EvaluationTurn t = turns.get(i);
                userMessageBuilder.append(String.format("Lượt %d (%s):\n", i + 1, t.turnType()));
                userMessageBuilder.append(" - Câu hỏi: ").append(t.questionText()).append("\n");
                userMessageBuilder.append(" - Câu trả lời: ").append(t.transcriptText() != null ? t.transcriptText() : "(Chưa có)").append("\n");
                userMessageBuilder.append(" - Điểm thành phần: ").append(t.score() != null ? t.score() : "0").append("\n");
                userMessageBuilder.append(" - Nhận xét: ").append(t.feedback() != null ? t.feedback() : "Không có").append("\n\n");
            }

            if (rubric != null) {
                userMessageBuilder.append("RUBRIC ĐÁNH GIÁ CHUNG:\n");
                userMessageBuilder.append(" - Tên: ").append(rubric.name()).append("\n");
                if (rubric.description() != null) {
                    userMessageBuilder.append(" - Chi tiết: ").append(rubric.description()).append("\n");
                }
            }

            var response = chatModel.chat(ChatRequest.builder().messages(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userMessageBuilder.toString()))
                    .build());

            String raw = response != null && response.aiMessage() != null ? response.aiMessage().text() : null;
            if (raw == null || raw.isBlank()) {
                throw new AiServiceException("Gemini returned empty grading response");
            }

            GradeAttemptLlmResponse llmPayload = objectMapper.readValue(extractJson(raw), GradeAttemptLlmResponse.class);

            Double rawTotal = llmPayload.totalScore();
            BigDecimal totalScore;
            if (rawTotal == null || !Double.isFinite(rawTotal)) {
                totalScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                double clamped = Math.max(0.0, Math.min(10.0, rawTotal));
                totalScore = BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
            }

            // The grade band is a business rule, not something the LLM decides: derive it from the score
            GradeLevel gradeLevel = GradeLevel.fromScore(totalScore);

            return new AttemptGradingResult(
                    totalScore,
                    gradeLevel,
                    llmPayload.detailedFeedback() != null ? llmPayload.detailedFeedback() : "",
                    ScoringMode.GEMINI_FLASH_LITE
            );

        } catch (Exception ex) {
            log.warn("Gemini attempt grading failed: {}, falling back to RuleBasedGraderFallback", ex.getMessage());
            return fallbackGrader.gradeAttempt(turns, rubric);
        }
    }

    private static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : raw;
    }
}
