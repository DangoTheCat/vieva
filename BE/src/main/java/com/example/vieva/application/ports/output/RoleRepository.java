package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findByRoleCodesIn(Collection<String> roleCodes);
    Role save(Role role);
}
