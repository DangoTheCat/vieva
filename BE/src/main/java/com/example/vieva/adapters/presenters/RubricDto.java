package com.example.vieva.adapters.presenters;

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
public class RubricDto {
    private UUID rubricId;
    private String rubricName;
    private BigDecimal totalPoints;
    private String description;
    private List<RubricCriterionDto> criteria;
}
