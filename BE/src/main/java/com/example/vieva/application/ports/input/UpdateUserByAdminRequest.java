package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Application-layer input DTO for updating a user by Admin.
 * Pure Java — free from presentation/framework validation annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserByAdminRequest {
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    /** Single role code (an account has exactly one role). */
    private String roleCode;
}
