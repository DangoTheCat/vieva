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
public class QuestionSourceDto {
    private UUID questionSourceId;
    private UUID chunkId;
    private String documentName;
    private String citationQuote;
    private BigDecimal similarityScore;
}
