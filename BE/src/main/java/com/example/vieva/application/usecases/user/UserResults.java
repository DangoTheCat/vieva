package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.user.UserResult;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;

import java.util.Collections;
import java.util.Set;

public final class UserResults {

    private UserResults() {
    }

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
