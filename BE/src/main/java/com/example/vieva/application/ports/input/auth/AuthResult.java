package com.example.vieva.application.ports.input.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Output DTO returned by auth use cases.
 * Lives in ports/input/auth as an application-layer response model.
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
    private String avatarUrl;
    private String status;
    private Set<String> roles;
    private boolean mustChangePassword;
    private Instant createdAt;
}
