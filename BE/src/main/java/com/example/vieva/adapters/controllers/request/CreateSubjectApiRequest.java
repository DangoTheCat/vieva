package com.example.vieva.adapters.controllers.request;

import com.example.vieva.domain.entities.SubjectStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectApiRequest {

    @NotBlank(message = "Subject code is required")
    @Size(max = 50, message = "Subject code must not exceed 50 characters")
    private String subjectCode;

    @NotBlank(message = "Subject name is required")
    @Size(max = 255, message = "Subject name must not exceed 255 characters")
    private String subjectName;

    private String description;

    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 30, message = "Credits must not exceed 30")
    @Builder.Default
    private Integer credits = 3;

    @Builder.Default
    private SubjectStatus status = SubjectStatus.ACTIVE;
}
