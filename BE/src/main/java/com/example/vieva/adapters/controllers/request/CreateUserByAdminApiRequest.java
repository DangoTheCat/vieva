package com.example.vieva.adapters.controllers.request;

import com.example.vieva.domain.entities.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Controller-layer request DTO for creating a user by Admin.
 * Handles HTTP payload validation before passing into application layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserByAdminApiRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+?[0-9]{9,11})$", message = "Phone number must contain between 9 and 11 digits")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 50, message = "User code must not exceed 50 characters")
    private String userCode;

    private UserStatus status;

    @Size(max = 20, message = "Cannot assign more than 20 roles at once")
    private Set<String> roleCodes;
}
