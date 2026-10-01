package com.example.vieva.application.ports.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignLecturerRequest {
    private UUID subjectId;
    private List<UUID> lecturerIds;
}
