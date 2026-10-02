package com.example.vieva.adapters.presenters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterionDto {
    private UUID criterionId;
    private String criterionName;
    private BigDecimal maxPoints;
    private String achievementDescriptors;
    private Integer orderIndex;
}
