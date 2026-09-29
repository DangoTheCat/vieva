package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-level request DTO with Jakarta Validation annotations.
 * Maps to application-layer LoginRequest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginApiRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
