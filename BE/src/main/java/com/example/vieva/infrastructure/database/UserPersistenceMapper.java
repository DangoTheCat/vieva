package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserPersistenceMapper {

    private final RolePersistenceMapper roleMapper;
    private final RoleJpaRepository roleJpaRepository;

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<UserRole> roles = entity.getUserRoles() == null ? Collections.emptySet() :
                entity.getUserRoles().stream()
                        .map(ur -> UserRole.builder()
                                .userId(ur.getId() != null ? ur.getId().getUserId() : entity.getUserId())
                                .roleId(ur.getId() != null ? ur.getId().getRoleId() : (ur.getRole() != null ? ur.getRole().getRoleId() : null))
                                .role(ur.getRole() != null ? roleMapper.toDomain(ur.getRole()) : null)
                                .assignedAt(ur.getAssignedAt())
                                .assignedBy(ur.getAssignedBy())
                                .build())
                        .collect(Collectors.toSet());

        return User.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .userCode(entity.getUserCode())
                .fullName(entity.getFullName())
                .passwordHash(entity.getPasswordHash())
                .phoneNumber(entity.getPhoneNumber())
                .status(entity.getStatus())
                .deletedAt(entity.getDeletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .userRoles(roles)
                .build();
    }

    public UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .userId(domain.getUserId())
                .email(domain.getEmail())
                .userCode(domain.getUserCode())
                .fullName(domain.getFullName())
                .passwordHash(domain.getPasswordHash())
                .phoneNumber(domain.getPhoneNumber())
                .status(domain.getStatus())
                .deletedAt(domain.getDeletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();

        if (domain.getUserRoles() != null && !domain.getUserRoles().isEmpty()) {
            Set<UserRoleJpaEntity> roleEntities = domain.getUserRoles().stream()
                    .map(ur -> {
                        Integer roleId = ur.getRoleId() != null ? ur.getRoleId() :
                                (ur.getRole() != null ? ur.getRole().getRoleId() : null);
                        UserRoleId compositeId = new UserRoleId(domain.getUserId(), roleId);

                        RoleJpaEntity roleEntity = null;
                        if (roleId != null) {
                            roleEntity = roleJpaRepository.findById(roleId).orElse(null);
                        }
                        if (roleEntity == null && ur.getRole() != null) {
                            roleEntity = roleMapper.toEntity(ur.getRole());
                        }

                        return UserRoleJpaEntity.builder()
                                .id(compositeId)
                                .user(entity)
                                .role(roleEntity)
                                .assignedAt(ur.getAssignedAt())
                                .assignedBy(ur.getAssignedBy())
                                .build();
                    })
                    .collect(Collectors.toSet());
            entity.setUserRoles(roleEntities);
        }

        return entity;
    }
}
