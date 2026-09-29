package com.example.vieva.adapters.controllers.request;

import com.example.vieva.domain.entities.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Controller-layer request DTO for updating a user by Admin.
 * Handles HTTP payload validation before passing into application layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserByAdminApiRequest {

    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+?[0-9]{9,11})$", message = "Phone number must contain between 9 and 11 digits")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    private UserStatus status;

    private Set<String> roleCodes;
}
