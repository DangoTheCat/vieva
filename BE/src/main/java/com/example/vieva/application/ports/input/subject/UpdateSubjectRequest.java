package com.example.vieva.application.ports.input.subject;

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
    private String status;
}
