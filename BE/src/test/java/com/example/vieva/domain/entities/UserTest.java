package com.example.vieva.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
