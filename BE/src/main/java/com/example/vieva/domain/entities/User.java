package com.example.vieva.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String passwordHash;
    private String phoneNumber;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public void addRole(Role role, UUID assignedBy) {
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        UserRole userRole = UserRole.builder()
                .userId(this.userId)
                .roleId(role.getRoleId())
                .role(role)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .build();
        userRoles.add(userRole);
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED || deletedAt != null;
    }

    public boolean hasRole(String roleCode) {
        if (userRoles == null || roleCode == null) {
            return false;
        }
        return userRoles.stream()
                .anyMatch(ur -> ur.getRole() != null && roleCode.equalsIgnoreCase(ur.getRole().getRoleCode()));
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ADMIN");
    }

    public void clearRoles() {
        if (userRoles != null) {
            userRoles.clear();
        }
    }
}
