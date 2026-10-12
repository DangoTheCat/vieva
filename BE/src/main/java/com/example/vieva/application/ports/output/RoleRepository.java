package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByRoleCode(String roleCode);
    Role save(Role role);
}
