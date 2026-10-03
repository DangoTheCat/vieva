package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.SubjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubjectRequest {
    private String subjectName;
    private String description;
    private Integer credits;
    private SubjectStatus status;
}
