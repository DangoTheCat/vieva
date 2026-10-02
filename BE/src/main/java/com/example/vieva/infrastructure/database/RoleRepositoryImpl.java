package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.domain.entities.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<Role> findByRoleCodesIn(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return roleJpaRepository.findByRoleCodeIn(roleCodes).stream()
                .map(roleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Role save(Role role) {
        return roleMapper.toDomain(roleJpaRepository.save(roleMapper.toEntity(role)));
    }
}
