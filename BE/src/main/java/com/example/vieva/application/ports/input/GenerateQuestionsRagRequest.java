package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsRagRequest {
    @NotNull(message = "Topic ID is required")
    private UUID topicId;

    @NotNull(message = "Bloom level is required")
    private BloomLevel bloomLevel;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Quantity cannot exceed 10")
    @Builder.Default
    private int quantity = 3;

    private String customPrompt;
}
