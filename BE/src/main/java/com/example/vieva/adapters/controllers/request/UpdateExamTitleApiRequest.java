package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Partial update of a DRAFT exam: any field may be omitted. A title, when sent, must not be blank.
 */
public record UpdateExamTitleApiRequest(
        @Pattern(regexp = ".*\\S.*", message = "Exam title cannot be blank")
        String title,

        @Min(value = 30, message = "Answer time limit must be at least 30 seconds")
        @Max(value = 600, message = "Answer time limit must be at most 600 seconds")
        Integer answerTimeLimitSeconds,

        @Min(value = 0, message = "Max follow-ups must be between 0 and 3")
        @Max(value = 3, message = "Max follow-ups must be between 0 and 3")
        Integer maxFollowUpsPerQuestion
) {}
