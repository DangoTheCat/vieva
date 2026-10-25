package com.example.vieva.application.ports.input.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRubricTemplateCommand {
    private String name;
    private String description;
    private String criteriaJson;
}
