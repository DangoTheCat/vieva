package com.example.vieva.application.ports.input;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterionInput {
    @NotBlank(message = "Criterion name is required")
    private String criterionName;

    @NotNull(message = "Max points is required")
    @DecimalMin(value = "0.1", message = "Max points must be greater than 0")
    private BigDecimal maxPoints;

    @NotBlank(message = "Achievement descriptors are required")
    private String achievementDescriptors;

    private Integer orderIndex;
}
