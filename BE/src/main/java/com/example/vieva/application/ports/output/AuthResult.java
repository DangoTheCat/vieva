package com.example.vieva.application.ports.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Output DTO returned by auth use cases.
 * Lives in the application layer so use cases don't depend on adapter-layer presenters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String phoneNumber;
    private String status;
    private Set<String> roles;
    private boolean mustChangePassword;
    private Instant createdAt;
}
