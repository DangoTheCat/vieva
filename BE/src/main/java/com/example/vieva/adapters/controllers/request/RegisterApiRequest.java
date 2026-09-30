package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-level request DTO with Jakarta Validation annotations.
 * Maps to application-layer RegisterRequest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterApiRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 50, message = "Full name must not exceed 50 characters")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+?[0-9]{9,11})$", message = "Phone number must contain between 9 and 11 digits")
    @Size(max = 12, message = "Phone number must not exceed 11 digits")
    private String phoneNumber;
}
