package com.example.vieva.application.ports.input.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResult {
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String status;
    private String role;
    private Set<String> roles;
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;
}
