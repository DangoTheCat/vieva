package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManualQuestionRequest {
    @NotNull(message = "Topic ID is required")
    private UUID topicId;

    @NotBlank(message = "Question content is required")
    private String questionContent;

    @NotBlank(message = "Reference answer is required")
    private String referenceAnswer;

    @NotNull(message = "Bloom level is required")
    private BloomLevel bloomLevel;

    @NotBlank(message = "Rubric name is required")
    private String rubricName;

    @NotNull(message = "Rubric total points is required")
    @DecimalMin(value = "0.1", message = "Total points must be greater than 0")
    private BigDecimal totalPoints;

    private String rubricDescription;

    @NotEmpty(message = "At least one rubric criterion is required")
    @Valid
    private List<RubricCriterionInput> criteria;
}
