package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AuditEventRepository auditEventRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<User> getUsers(UserSearchCriteria criteria) {
        return userRepository.findAll(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    @Override
    public User createUser(CreateUserByAdminRequest request, UUID currentAdminId) {
        String email = new Email(request.getEmail()).getValue();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        String userCode;
        if (request.getUserCode() != null && !request.getUserCode().trim().isEmpty()) {
            userCode = request.getUserCode().trim();
            if (userRepository.existsByUserCode(userCode)) {
                throw new AppException(ErrorCode.USER_CODE_EXISTED);
            }
        } else {
            userCode = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        UserStatus status = request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE;
        if (status == UserStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Set<Role> rolesToAssign = resolveRoles(request.getRoleCodes());

        UUID newUserId = UUID.randomUUID();
        User user = User.builder()
                .userId(newUserId)
                .email(email)
                .userCode(userCode)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        for (Role role : rolesToAssign) {
            user.addRole(role, currentAdminId);
        }

        User savedUser = userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_CREATED")
                .entityType("USER")
                .entityId(savedUser.getUserId().toString())
                .newValuesJson(String.format("{\"email\":\"%s\",\"userCode\":\"%s\",\"status\":\"%s\"}",
                        savedUser.getEmail(), savedUser.getUserCode(), savedUser.getStatus()))
                .createdAt(Instant.now())
                .build());

        return savedUser;
    }

    @Override
    public User updateUser(UUID targetUserId, UpdateUserByAdminRequest request, UUID currentAdminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Rule: PUT cannot set status = DELETED
        if (request.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        boolean isTargetAdmin = user.isAdmin();

        // Rule: ADMIN cannot self-lock / deactivate
        if (targetUserId.equals(currentAdminId) && request.getStatus() != null && request.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.CANNOT_LOCK_SELF);
        }

        // Rule: ADMIN cannot lock / deactivate the last active ADMIN
        // Use FOR UPDATE lock to serialize concurrent demote attempts
        if (isTargetAdmin && request.getStatus() != null && request.getStatus() != UserStatus.ACTIVE) {
            if (userRepository.countActiveAdminsForUpdate() <= 1) {
                throw new AppException(ErrorCode.CANNOT_LOCK_LAST_ADMIN);
            }
        }

        // Rule: ADMIN cannot self-demote
        if (targetUserId.equals(currentAdminId) && request.getRoleCodes() != null) {
            boolean willRemainAdmin = willHaveAdminRole(request.getRoleCodes());
            if (!willRemainAdmin) {
                throw new AppException(ErrorCode.CANNOT_DEMOTE_SELF);
            }
        }

        // Rule: ADMIN cannot demote the last ADMIN
        // Use FOR UPDATE lock to serialize concurrent demote attempts
        if (isTargetAdmin && request.getRoleCodes() != null) {
            boolean willRemainAdmin = willHaveAdminRole(request.getRoleCodes());
            if (!willRemainAdmin && userRepository.countActiveAdminsForUpdate() <= 1) {
                throw new AppException(ErrorCode.CANNOT_DEMOTE_LAST_ADMIN);
            }
        }

        // Apply field updates
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getRoleCodes() != null) {
            Set<Role> updatedRoles = resolveRoles(request.getRoleCodes());
            user.clearRoles();
            for (Role role : updatedRoles) {
                user.addRole(role, currentAdminId);
            }
        }

        user.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_UPDATED")
                .entityType("USER")
                .entityId(updatedUser.getUserId().toString())
                .newValuesJson(String.format("{\"status\":\"%s\"}", updatedUser.getStatus()))
                .createdAt(Instant.now())
                .build());

        return updatedUser;
    }

    @Override
    public void deleteUser(UUID targetUserId, UUID currentAdminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Rule: Trying to delete an already DELETED user returns 404
        if (user.isDeleted()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Rule: ADMIN cannot delete self
        if (targetUserId.equals(currentAdminId)) {
            throw new AppException(ErrorCode.CANNOT_DELETE_SELF);
        }

        // Rule: ADMIN cannot delete the last active ADMIN
        // Use FOR UPDATE lock to serialize concurrent delete attempts at the DB level
        if (user.isAdmin() && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countActiveAdminsForUpdate() <= 1) {
            throw new AppException(ErrorCode.CANNOT_DELETE_LAST_ADMIN);
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_DELETED")
                .entityType("USER")
                .entityId(user.getUserId().toString())
                .newValuesJson("{\"status\":\"DELETED\"}")
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public void resetPassword(UUID targetUserId, String newPassword, UUID currentAdminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (!StringUtils.hasText(newPassword) || newPassword.trim().length() < 6) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordChangedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_PASSWORD_RESET")
                .entityType("USER")
                .entityId(user.getUserId().toString())
                .newValuesJson("{\"passwordReset\":true}")
                .createdAt(Instant.now())
                .build());
    }

    /**
     * Resolve a set of role codes to Role entities.
     * Uses a single findByRoleCode per unique code (not in a large unbounded loop).
     * Null/blank codes in the set are silently skipped.
     *
     * @param roleCodes set of role code strings (max 20 enforced at controller layer)
     */
    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        if (roleCodes == null || roleCodes.isEmpty()) {
            Role defaultRole = roleRepository.findByRoleCode("ROLE_USER")
                    .or(() -> roleRepository.findByRoleCode("USER"))
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(defaultRole);
            return roles;
        }

        for (String code : roleCodes) {
            // Guard: skip null or blank entries before calling trim()
            if (!StringUtils.hasText(code)) {
                continue;
            }
            String cleanCode = code.trim();
            Role role = roleRepository.findByRoleCode(cleanCode)
                    .or(() -> roleRepository.findByRoleCode(cleanCode.startsWith("ROLE_") ? cleanCode.substring(5) : "ROLE_" + cleanCode))
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(role);
        }

        if (roles.isEmpty()) {
            Role defaultRole = roleRepository.findByRoleCode("ROLE_USER")
                    .or(() -> roleRepository.findByRoleCode("USER"))
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(defaultRole);
        }

        return roles;
    }

    /**
     * Check if the new set of role codes includes an admin role.
     * Null codes in the set are filtered out before calling trim().
     */
    private boolean willHaveAdminRole(Set<String> roleCodes) {
        if (roleCodes == null) return false;
        return roleCodes.stream()
                .filter(code -> code != null)   // guard: skip null entries
                .anyMatch(code ->
                        "ROLE_ADMIN".equalsIgnoreCase(code.trim()) || "ADMIN".equalsIgnoreCase(code.trim())
                );
    }
}
