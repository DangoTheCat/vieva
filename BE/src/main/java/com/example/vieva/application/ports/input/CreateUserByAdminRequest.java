package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

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
    private String password;
    private String fullName;
    private String phoneNumber;
    private String userCode;
    private UserStatus status;
    private Set<String> roleCodes;
}
