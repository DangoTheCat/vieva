package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.output.AuthResult;
import org.springframework.stereotype.Component;

/**
 * Maps application-layer AuthResult to adapter-layer AuthResponse for API output.
 */
@Component
public class AuthPresenter {

    public AuthResponse toResponse(AuthResult result) {
        if (result == null) {
            return null;
        }

        UserDto userDto = UserDto.builder()
                .userId(result.getUserId())
                .email(result.getEmail())
                .userCode(result.getUserCode())
                .fullName(result.getFullName())
                .phoneNumber(result.getPhoneNumber())
                .status(result.getStatus() != null
                        ? com.example.vieva.domain.entities.UserStatus.valueOf(result.getStatus())
                        : null)
                .role(result.getRoles() != null ? result.getRoles().stream().findFirst().orElse(null) : null)
                .roles(result.getRoles())
                .mustChangePassword(result.isMustChangePassword())
                .createdAt(result.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(result.getAccessToken())
                .tokenType(result.getTokenType())
                .expiresIn(result.getExpiresIn())
                .user(userDto)
                .build();
    }
}
