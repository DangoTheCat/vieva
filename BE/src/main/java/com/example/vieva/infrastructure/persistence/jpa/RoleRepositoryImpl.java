package com.example.vieva.infrastructure.persistence.jpa;

import com.example.vieva.domain.entity.Role;
import com.example.vieva.domain.repository.RoleRepository;
import com.example.vieva.infrastructure.persistence.mapper.RolePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;
    private final RolePersistenceMapper roleMapper;

    @Override
    public Optional<Role> findById(Integer roleId) {
        return roleJpaRepository.findById(roleId)
                .map(roleMapper::toDomain);
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return roleJpaRepository.findByRoleCode(roleCode)
                .map(roleMapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return roleJpaRepository.findAll().stream()
                .map(roleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = roleMapper.toEntity(role);
        RoleJpaEntity saved = roleJpaRepository.save(entity);
        return roleMapper.toDomain(saved);
    }
}
