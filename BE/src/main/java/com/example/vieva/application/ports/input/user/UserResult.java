package com.example.vieva.application.ports.input.user;

import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResult {
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String status;
    private String role;
    private Set<String> roles;
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResult from(User user) {
        if (user == null) {
            return null;
        }
        Role role = user.getRole();
        String roleCode = role != null ? role.getRoleCode() : null;
        Set<String> roles = roleCode != null ? Set.of(roleCode) : Collections.emptySet();
        return UserResult.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .role(roleCode)
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
