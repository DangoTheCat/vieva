package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AssignLecturerApiRequest {

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotEmpty(message = "At least one lecturer ID must be provided")
    @Size(max = 100, message = "At most 100 lecturer IDs may be assigned in one request")
    private List<UUID> lecturerIds;
}
