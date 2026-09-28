package com.example.vieva.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    private UUID userId;
    private Integer roleId;
    private Role role;
    private Instant assignedAt;
    private UUID assignedBy;
}
