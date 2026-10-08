package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AuditEventRepository auditEventRepository;
    private final JsonSerializerPort jsonSerializer;
    private final DomainEventPublisherPort domainEventPublisher;

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

        Role roleToAssign = resolveRole(request.getRoleCode());

        // Admin-created accounts get a temporary password that must be changed on first login
        String temporaryPassword = StringUtils.hasText(request.getPassword())
                ? request.getPassword()
                : TemporaryPasswordGenerator.generate();

        UUID newUserId = UUID.randomUUID();
        User user = User.builder()
                .userId(newUserId)
                .email(email)
                .userCode(userCode)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .status(status)
                .mustChangePassword(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user.assignRole(roleToAssign, currentAdminId);

        final User savedUser;
        try {
            // Flush forces the INSERT now so a concurrent duplicate surfaces here (unique indexes)
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Concurrent creation with the same email/userCode won the race (unique indexes)
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Map<String, Object> auditValues = new HashMap<>();
        auditValues.put("email", savedUser.getEmail());
        auditValues.put("userCode", savedUser.getUserCode());
        auditValues.put("status", savedUser.getStatus() != null ? savedUser.getStatus().name() : null);
        auditValues.put("role", roleToAssign.getRoleCode());

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_CREATED")
                .entityType("USER")
                .entityId(savedUser.getUserId().toString())
                .newValuesJson(jsonSerializer.serialize(auditValues))
                .createdAt(Instant.now())
                .build());

        // Sent after commit by the async email worker
        domainEventPublisher.publishAccountCreated(savedUser.getEmail(), savedUser.getFullName(),
                roleToAssign.getRoleCode(), temporaryPassword);

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
        if (isTargetAdmin && user.getStatus() == UserStatus.ACTIVE
                && request.getStatus() != null && request.getStatus() != UserStatus.ACTIVE) {
            if (userRepository.countActiveAdmins() <= 1) {
                throw new AppException(ErrorCode.CANNOT_LOCK_LAST_ADMIN);
            }
        }

        // Rule: ADMIN cannot self-demote
        if (targetUserId.equals(currentAdminId) && request.getRoleCode() != null) {
            boolean willRemainAdmin = isAdminRoleCode(request.getRoleCode());
            if (!willRemainAdmin) {
                throw new AppException(ErrorCode.CANNOT_DEMOTE_SELF);
            }
        }

        // Rule: ADMIN cannot demote the last ADMIN
        // Use FOR UPDATE lock to serialize concurrent demote attempts
        if (isTargetAdmin && user.getStatus() == UserStatus.ACTIVE && request.getRoleCode() != null) {
            boolean willRemainAdmin = isAdminRoleCode(request.getRoleCode());
            if (!willRemainAdmin && userRepository.countActiveAdmins() <= 1) {
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
        if (request.getRoleCode() != null) {
            // Replaces the current role: an account has exactly one
            user.assignRole(resolveRole(request.getRoleCode()), currentAdminId);
        }

        user.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_UPDATED")
                .entityType("USER")
                .entityId(updatedUser.getUserId().toString())
                .newValuesJson(jsonSerializer.serialize(Map.of("status", updatedUser.getStatus().name())))
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
                && userRepository.countActiveAdmins() <= 1) {
            throw new AppException(ErrorCode.CANNOT_DELETE_LAST_ADMIN);
        }

        user.delete();
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_DELETED")
                .entityType("USER")
                .entityId(user.getUserId().toString())
                .newValuesJson(jsonSerializer.serialize(Map.of("status", UserStatus.DELETED.name())))
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

        // The admin knows this password, so the user must replace it on next login
        user.issueTemporaryPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("USER_PASSWORD_RESET")
                .entityType("USER")
                .entityId(user.getUserId().toString())
                .newValuesJson(jsonSerializer.serialize(Map.of("passwordReset", true)))
                .createdAt(Instant.now())
                .build());
    }

    /**
     * Resolve a single role code (ROLE_ADMIN, ROLE_LECTURER or ROLE_STUDENT) to its Role.
     * The {@code ROLE_} prefix is optional ({@code LECTURER} works); a blank code means ROLE_STUDENT.
     */
    private Role resolveRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return findDefaultRole();
        }
        return roleRepository.findByRoleCode(canonicalRoleCode(roleCode))
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private Role findDefaultRole() {
        return roleRepository.findByRoleCode("ROLE_STUDENT")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private boolean isAdminRoleCode(String roleCode) {
        return "ROLE_ADMIN".equals(canonicalRoleCode(roleCode));
    }

    private String canonicalRoleCode(String roleCode) {
        String cleanCode = roleCode.trim().toUpperCase(Locale.ROOT);
        return cleanCode.startsWith("ROLE_") ? cleanCode : "ROLE_" + cleanCode;
    }
}
