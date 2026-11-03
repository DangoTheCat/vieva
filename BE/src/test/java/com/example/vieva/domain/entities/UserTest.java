package com.example.vieva.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("updateProfile: updates fullName, phoneNumber and updatedAt")
    void updateProfileUpdatesFields() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Old Name")
                .phoneNumber("0123456789")
                .status(UserStatus.ACTIVE)
                .build();

        Instant now = Instant.now();
        user.updateProfile("New Name", "0987654321", now);

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("changeStatus: updates status and updatedAt")
    void changeStatusUpdatesFields() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .status(UserStatus.ACTIVE)
                .build();

        Instant now = Instant.now();
        user.changeStatus(UserStatus.INACTIVE, now);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("User value-based equals and hashCode")
    void userEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User u1 = User.builder()
                .userId(id)
                .email("test@example.com")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .build();

        User u2 = User.builder()
                .userId(id)
                .email("test@example.com")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .build();

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    @DisplayName("Role value-based equals and hashCode")
    void roleEqualsAndHashCode() {
        Role r1 = Role.builder()
                .roleId(1)
                .roleCode("ROLE_LECTURER")
                .roleName("Lecturer")
                .build();

        Role r2 = Role.builder()
                .roleId(1)
                .roleCode("ROLE_LECTURER")
                .roleName("Lecturer")
                .build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("getUserRoles returns an unmodifiable set protecting encapsulation")
    void getUserRolesIsUnmodifiable() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("encapsulation@example.com")
                .build();

        Role role = Role.builder()
                .roleId(1)
                .roleCode("ROLE_STUDENT")
                .roleName("Student")
                .build();

        user.assignRole(role, userId);
        Set<UserRole> roles = user.getUserRoles();

        UserRole externalRole = UserRole.builder()
                .userId(userId)
                .roleId(2)
                .role(Role.builder().roleId(2).roleCode("ROLE_ADMIN").roleName("Admin").build())
                .build();

        assertThatThrownBy(() -> roles.add(externalRole))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(roles::clear)
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(user.getUserRoles()).hasSize(1);
    }

    @Test
    @DisplayName("getUserRoles returns empty set when internal set is null and cannot be mutated")
    void getUserRolesHandlesNullSafely() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .userRoles(null)
                .build();

        Set<UserRole> roles = user.getUserRoles();
        assertThat(roles).isNotNull().isEmpty();

        UserRole externalRole = UserRole.builder().roleId(1).build();
        assertThatThrownBy(() -> roles.add(externalRole))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("assignRole modifies internal roles correctly while maintaining business invariants")
    void assignRoleModifiesRolesCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("student@example.com")
                .build();

        Role studentRole = Role.builder()
                .roleId(1)
                .roleCode("ROLE_STUDENT")
                .roleName("Student")
                .build();

        Role lecturerRole = Role.builder()
                .roleId(2)
                .roleCode("ROLE_LECTURER")
                .roleName("Lecturer")
                .build();

        // Assign student role
        user.assignRole(studentRole, adminId);
        assertThat(user.getUserRoles()).hasSize(1);
        assertThat(user.getRole()).isEqualTo(studentRole);
        assertThat(user.isStudent()).isTrue();
        assertThat(user.isLecturer()).isFalse();

        // Reassigning replaces current role (single role invariant)
        user.assignRole(lecturerRole, adminId);
        assertThat(user.getUserRoles()).hasSize(1);
        assertThat(user.getRole()).isEqualTo(lecturerRole);
        assertThat(user.isStudent()).isFalse();
        assertThat(user.isLecturer()).isTrue();
    }
}
