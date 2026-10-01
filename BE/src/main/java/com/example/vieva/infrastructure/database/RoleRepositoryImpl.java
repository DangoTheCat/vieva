package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.domain.entities.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;
    private final RolePersistenceMapper roleMapper;

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return roleJpaRepository.findByRoleCode(roleCode)
                .map(roleMapper::toDomain);
    }

    @Override
    @Transactional
    public Role save(Role role) {
        return roleMapper.toDomain(roleJpaRepository.save(roleMapper.toEntity(role)));
    }
}
