package com.example.vieva.adapters.controllers.request;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.PerformanceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request bodies of the group-1 (question bank) API. Syntactic validation here; business rules
 * (rubric totals, Bloom confirmation, sources...) are enforced by the use cases.
 */
public final class QuestionBankApiRequests {

    private QuestionBankApiRequests() {
    }

    public record PerformanceLevelRequest(
            @NotBlank(message = "Level label is required") @Size(max = 100) String label,
            @Size(max = 1000) String description,
            @NotNull(message = "Level score is required") @DecimalMin("0") BigDecimal score) {

        PerformanceLevel toDomain() {
            return new PerformanceLevel(label.trim(), description == null ? null : description.trim(), score);
        }
    }

    /**
     * @param expectedVersion lock token of the question version (used by the criterion endpoints)
     */
    public record CriterionRequest(
            @NotBlank(message = "Criterion name is required") @Size(max = 255) String name,
            @NotBlank(message = "Criterion description is required") @Size(max = 4000) String description,
            @NotNull(message = "Criterion max score is required")
            @DecimalMin(value = "0", inclusive = false, message = "Criterion max score must be greater than 0")
            @DecimalMax(value = "999.99", message = "Criterion max score must not exceed 999.99") BigDecimal maxScore,
            @Valid @Size(max = 10) List<PerformanceLevelRequest> levels,
            @Min(1) Integer orderIndex,
            Long expectedVersion) {

        public CriterionInput toInput() {
            return new CriterionInput(name, description, maxScore,
                    levels == null ? List.of() : levels.stream().map(PerformanceLevelRequest::toDomain).toList(),
                    orderIndex);
        }
    }

    /**
     * @param totalScore optional; when sent it must equal Σ criteria max scores
     */
    public record RubricRequest(
            @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @DecimalMin(value = "0", inclusive = false) BigDecimal totalScore,
            @NotEmpty(message = "At least one rubric criterion is required") @Size(max = 20) @Valid
            List<CriterionRequest> criteria,
            Long expectedVersion) {

        public RubricInput toInput() {
            return new RubricInput(name, description, totalScore,
                    criteria.stream().map(CriterionRequest::toInput).toList());
        }
    }

    public record CreateManualQuestionRequest(
            UUID topicId,
            @NotBlank(message = "Question content is required") @Size(max = 4000) String content,
            @NotBlank(message = "Expected answer is required") @Size(max = 8000) String expectedAnswer,
            @NotNull(message = "Bloom level is required") BloomLevel bloomLevel,
            @NotNull(message = "Rubric is required") @Valid RubricRequest rubric) {
    }

    /**
     * Partial update of a DRAFT; null fields stay unchanged. {@code bloomConfirmed=true} confirms the
     * Bloom level suggested by AI (BR-01).
     */
    public record UpdateDraftRequest(
            UUID topicId,
            @Size(max = 4000) String content,
            @Size(max = 8000) String expectedAnswer,
            BloomLevel bloomLevel,
            Boolean bloomConfirmed,
            @Valid RubricRequest rubric,
            Long expectedVersion) {
    }

    public record ConfirmBloomRequest(
            @NotNull(message = "Bloom level is required") BloomLevel bloomLevel,
            Long expectedVersion) {
    }

    public record ApproveRequest(Long expectedVersion) {
    }

    public record RejectRequest(
            @NotBlank(message = "Rejection reason is required") @Size(max = 2000) String reason,
            Long expectedVersion) {
    }

    public record RegenerateRequest(
            @Size(max = 2000) String feedback,
            Long expectedVersion) {
    }

    /**
     * @param bloomDistribution number of questions per Bloom level; must add up to {@code totalQuestions}
     */
    public record GenerateQuestionsRequest(
            @NotNull(message = "subjectId is required") UUID subjectId,
            UUID topicId,
            @NotEmpty(message = "Select at least one READY document") @Size(max = 20) List<@NotNull UUID> documentIds,
            @Min(value = 1, message = "totalQuestions must be at least 1")
            @Max(value = 50, message = "totalQuestions must not exceed 50") int totalQuestions,
            @NotEmpty(message = "Bloom distribution is required") Map<@NotNull BloomLevel, @NotNull @Min(0) Integer> bloomDistribution,
            @Size(max = 2000) String lecturerNote) {
    }

    public record CreateTopicRequest(
            @NotBlank(message = "Topic name is required") @Size(max = 255) String name,
            @Size(max = 2000) String description) {
    }
}
