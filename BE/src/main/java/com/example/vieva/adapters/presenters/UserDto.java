package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.UserStatus;
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
public class UserDto {
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    /** The account's single role code. */
    private String role;
    /** Same role as a one-element set; kept for existing clients. */
    private Set<String> roles;
    /** True until the user replaces the password an admin set; the client must force a password change. */
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;
}
