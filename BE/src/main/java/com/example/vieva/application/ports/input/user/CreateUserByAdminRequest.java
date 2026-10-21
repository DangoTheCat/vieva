package com.example.vieva.application.ports.input.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Application-layer input DTO for creating a user by Admin.
 * Pure Java — free from presentation/framework validation annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserByAdminRequest {
    private String email;
    /** Temporary password; generated when blank. The user must change it on first login. */
    private String password;
    private String fullName;
    private String phoneNumber;
    private String userCode;
    private String status;
    /** Single role code (an account has exactly one role). */
    private String roleCode;
}
