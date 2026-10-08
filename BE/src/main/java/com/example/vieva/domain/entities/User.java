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
    private Instant passwordChangedAt;
    private Long version;

    /** Set when an admin created the account or reset its password; cleared by the user's own password change. */
    private boolean mustChangePassword;

    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * An account has exactly one role: assigning a role replaces the current one.
     */
    public void assignRole(Role role, UUID assignedBy) {
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        userRoles.clear();
        UserRole userRole = UserRole.builder()
                .userId(this.userId)
                .roleId(role.getRoleId())
                .role(role)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .build();
        userRoles.add(userRole);
    }

    /** The account's single role, or null when none is assigned. */
    public Role getRole() {
        if (userRoles == null) {
            return null;
        }
        return userRoles.stream()
                .map(UserRole::getRole)
                .filter(role -> role != null)
                .findFirst()
                .orElse(null);
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

    public boolean isLecturer() {
        return hasRole("ROLE_LECTURER") || hasRole("LECTURER");
    }

    public boolean isStudent() {
        return hasRole("ROLE_STUDENT") || hasRole("STUDENT");
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Password chosen by the user: lifts the forced-change requirement. */
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.mustChangePassword = false;
    }

    /** Password set by an admin: the user must replace it on next login. */
    public void issueTemporaryPassword(String passwordHash) {
        updatePassword(passwordHash);
        this.mustChangePassword = true;
    }
}
