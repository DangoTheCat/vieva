package com.example.vieva.domain.repository;

import com.example.vieva.domain.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findById(Integer roleId);
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findAll();
    Role save(Role role);
}
