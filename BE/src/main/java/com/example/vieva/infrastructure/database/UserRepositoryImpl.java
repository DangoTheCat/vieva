package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userMapper;

    @Override
    public Optional<User> findById(UUID userId) {
        return userJpaRepository.findById(userId)
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAllByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userJpaRepository.findByUserIdIn(userIds).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUserCode(String userCode) {
        return userJpaRepository.findByUserCode(userCode)
                .map(userMapper::toDomain);
    }

    @Override
    public PagedResult<User> findAll(UserSearchCriteria criteria) {
        int page = criteria.getSanitizedPage();
        int size = criteria.getSanitizedSize();
        String sortBy = criteria.getSanitizedSortBy();
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<UserJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Defensive soft-delete filter in addition to @SQLRestriction
            predicates.add(cb.isNull(root.get("deletedAt")));

            // 1. Filter by status if specified
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            // 2. Filter by role if specified
            if (StringUtils.hasText(criteria.getRole())) {
                // Use Locale.ROOT for locale-independent case conversion
                String roleCode = criteria.getRole().trim().toUpperCase(Locale.ROOT);
                String roleWithPrefix = roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;

                Join<UserJpaEntity, UserRoleJpaEntity> userRoleJoin = root.join("userRoles", JoinType.INNER);
                Join<UserRoleJpaEntity, RoleJpaEntity> roleJoin = userRoleJoin.join("role", JoinType.INNER);

                predicates.add(cb.or(
                        cb.equal(cb.upper(roleJoin.get("roleCode")), roleCode),
                        cb.equal(cb.upper(roleJoin.get("roleCode")), roleWithPrefix)
                ));

                if (query != null) {
                    query.distinct(true);
                }
            }

            // 3. Filter by search keyword across email, fullName, userCode (escaped)
            String escapedKeyword = criteria.getEscapedKeyword();
            if (StringUtils.hasText(escapedKeyword)) {
                // Use Locale.ROOT for locale-independent case conversion
                String pattern = "%" + escapedKeyword.toLowerCase(Locale.ROOT) + "%";
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern, '\\');
                Predicate fullNameLike = cb.like(cb.lower(root.get("fullName")), pattern, '\\');
                Predicate userCodeLike = cb.like(cb.lower(root.get("userCode")), pattern, '\\');
                predicates.add(cb.or(emailLike, fullNameLike, userCodeLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserJpaEntity> userPage = userJpaRepository.findAll(spec, pageRequest);

        List<User> userList = userPage.getContent().stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());

        return PagedResult.of(
                userList,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public long countActiveAdmins() {
        // Acquires PESSIMISTIC_WRITE (SELECT FOR UPDATE) on entity rows — avoids PostgreSQL aggregate lock error
        return userJpaRepository.findActiveAdminsForUpdate().stream()
                .map(UserJpaEntity::getUserId)
                .distinct()
                .count();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUserCode(String userCode) {
        return userJpaRepository.existsByUserCode(userCode);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userMapper.toDomain(userJpaRepository.save(userMapper.toEntity(user)));
    }

    @Override
    @Transactional
    public User saveAndFlush(User user) {
        return userMapper.toDomain(userJpaRepository.saveAndFlush(userMapper.toEntity(user)));
    }
}
