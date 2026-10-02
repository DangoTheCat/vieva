package com.example.vieva.adapters.controllers.request;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionDraftApiRequest {
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
    private List<RubricCriterionApiRequest> criteria;
}
