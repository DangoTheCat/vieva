package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain object representing the assignment of a Role to a User.
 *
 * NOTE: equals/hashCode are explicitly based on (userId, roleId) — NOT on assignedAt.
 * Using Lombok data generation would generate equals/hashCode from all fields including assignedAt,
 * which would allow the same role to be added twice (violating the composite PK).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    private UUID userId;
    private Integer roleId;
    private Role role;
    private Instant assignedAt;
    private UUID assignedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRole that = (UserRole) o;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}

