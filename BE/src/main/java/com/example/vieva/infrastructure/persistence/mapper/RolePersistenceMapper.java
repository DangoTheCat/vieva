package com.example.vieva.infrastructure.persistence.mapper;

import com.example.vieva.domain.entity.Role;
import com.example.vieva.infrastructure.persistence.jpa.RoleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Role.builder()
                .roleId(entity.getRoleId())
                .roleCode(entity.getRoleCode())
                .roleName(entity.getRoleName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RoleJpaEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }
        return RoleJpaEntity.builder()
                .roleId(domain.getRoleId())
                .roleCode(domain.getRoleCode())
                .roleName(domain.getRoleName())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
