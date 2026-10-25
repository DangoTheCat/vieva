package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTextQuestionsApiRequest {
    @NotBlank(message = "Question text cannot be blank")
    private String text;
    private UUID topicId;
    private String bloomLevel;
    private UUID rubricTemplateId;
}
