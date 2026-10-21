package com.example.vieva.application.usecases.user;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.user.AdminUserService;
import com.example.vieva.application.ports.input.user.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.user.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.user.UserResult;
import com.example.vieva.application.ports.input.user.UserSearchCriteria;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.ports.input.common.PageResult;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.common.EnumValues;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.valueobjects.Email;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@UseCaseTransaction
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final QuestionBankAuditor auditor;
    private final DomainEventPublisherPort domainEventPublisher;

    @Override
    @UseCaseTransaction(readOnly = true)
    public PageResult<UserResult> getUsers(UserSearchCriteria criteria) {
        if (criteria != null && criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            EnumValues.parse(UserStatus.class, criteria.getStatus(), "Invalid value for parameter 'status'");
        }
        PageResult<User> paged = userRepository.findAll(criteria);
        List<UserResult> content = paged.getContent().stream()
                .map(UserResults::from)
                .toList();
        return PageResult.<UserResult>builder()
                .content(content)
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .isFirst(paged.isFirst())
                .isLast(paged.isLast())
                .build();
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public UserResult getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        return UserResults.from(user);
    }

    @Override
    public UserResult createUser(CreateUserByAdminRequest request, UUID currentAdminId) {
        UserStatus parsedStatus = EnumValues.parse(UserStatus.class, request.getStatus(), "Malformed request body");
        String email = new Email(request.getEmail()).getValue();
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.USER_EXISTED);
        }

        String userCode;
        if (request.getUserCode() != null && !request.getUserCode().trim().isEmpty()) {
            userCode = request.getUserCode().trim();
            if (userRepository.existsByUserCode(userCode)) {
                throw new ApplicationException(ErrorCode.USER_CODE_EXISTED);
            }
        } else {
            userCode = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        UserStatus status = parsedStatus != null ? parsedStatus : UserStatus.ACTIVE;
        if (status == UserStatus.DELETED) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        Role roleToAssign = resolveRole(request.getRoleCode());

        // Admin-created accounts get a temporary password that must be changed on first login
        String temporaryPassword = request.getPassword() != null && !request.getPassword().isBlank()
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
        } catch (DuplicateKeyException e) {
            // Concurrent creation with the same email/userCode won the race (unique indexes)
            throw new ApplicationException(ErrorCode.USER_EXISTED);
        }

        Map<String, Object> auditValues = new HashMap<>();
        auditValues.put("email", savedUser.getEmail());
        auditValues.put("userCode", savedUser.getUserCode());
        auditValues.put("status", savedUser.getStatus() != null ? savedUser.getStatus().name() : null);
        auditValues.put("role", roleToAssign.getRoleCode());

        auditor.record(currentAdminId, "USER_CREATED", "USER", savedUser.getUserId(), auditValues);

        // Sent after commit by the async email worker
        domainEventPublisher.publishAccountCreated(savedUser.getEmail(), savedUser.getFullName(),
                roleToAssign.getRoleCode(), temporaryPassword);

        return UserResults.from(savedUser);
    }

    @Override
    public UserResult updateUser(UUID targetUserId, UpdateUserByAdminRequest request, UUID currentAdminId) {
        UserStatus targetStatus = EnumValues.parse(UserStatus.class, request.getStatus(), "Malformed request body");
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        // Rule: PUT cannot set status = DELETED
        if (targetStatus == UserStatus.DELETED) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        boolean isTargetAdmin = user.isAdmin();

        // Rule: ADMIN cannot self-lock / deactivate
        if (targetUserId.equals(currentAdminId) && targetStatus != null && targetStatus != UserStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.CANNOT_LOCK_SELF);
        }

        // Rule: ADMIN cannot lock / deactivate the last active ADMIN
        // Use FOR UPDATE lock to serialize concurrent demote attempts
        if (isTargetAdmin && user.getStatus() == UserStatus.ACTIVE
                && targetStatus != null && targetStatus != UserStatus.ACTIVE) {
            if (userRepository.countActiveAdmins() <= 1) {
                throw new ApplicationException(ErrorCode.CANNOT_LOCK_LAST_ADMIN);
            }
        }

        // Rule: ADMIN cannot self-demote
        if (targetUserId.equals(currentAdminId) && request.getRoleCode() != null) {
            boolean willRemainAdmin = isAdminRoleCode(request.getRoleCode());
            if (!willRemainAdmin) {
                throw new ApplicationException(ErrorCode.CANNOT_DEMOTE_SELF);
            }
        }

        // Rule: ADMIN cannot demote the last ADMIN
        // Use FOR UPDATE lock to serialize concurrent demote attempts
        if (isTargetAdmin && user.getStatus() == UserStatus.ACTIVE && request.getRoleCode() != null) {
            boolean willRemainAdmin = isAdminRoleCode(request.getRoleCode());
            if (!willRemainAdmin && userRepository.countActiveAdmins() <= 1) {
                throw new ApplicationException(ErrorCode.CANNOT_DEMOTE_LAST_ADMIN);
            }
        }

        // Apply field updates
        Instant now = Instant.now();
        String newFullName = user.getFullName();
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            newFullName = request.getFullName().trim();
        }
        String newPhone = user.getPhoneNumber();
        if (request.getPhoneNumber() != null) {
            newPhone = request.getPhoneNumber().trim();
        }
        user.updateProfile(newFullName, newPhone, now);

        if (targetStatus != null) {
            user.changeStatus(targetStatus, now);
        }
        if (request.getRoleCode() != null) {
            // Replaces the current role: an account has exactly one
            user.assignRole(resolveRole(request.getRoleCode()), currentAdminId);
        }

        User updatedUser = userRepository.save(user);

        auditor.record(currentAdminId, "USER_UPDATED", "USER", updatedUser.getUserId(),
                Map.of("status", updatedUser.getStatus().name()));

        return UserResults.from(updatedUser);
    }

    @Override
    public void deleteUser(UUID targetUserId, UUID currentAdminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        // Rule: Trying to delete an already DELETED user returns 404
        if (user.isDeleted()) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        // Rule: ADMIN cannot delete self
        if (targetUserId.equals(currentAdminId)) {
            throw new ApplicationException(ErrorCode.CANNOT_DELETE_SELF);
        }

        // Rule: ADMIN cannot delete the last active ADMIN
        // Use FOR UPDATE lock to serialize concurrent delete attempts at the DB level
        if (user.isAdmin() && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countActiveAdmins() <= 1) {
            throw new ApplicationException(ErrorCode.CANNOT_DELETE_LAST_ADMIN);
        }

        user.delete();
        userRepository.save(user);

        auditor.record(currentAdminId, "USER_DELETED", "USER", user.getUserId(),
                Map.of("status", UserStatus.DELETED.name()));
    }

    @Override
    public void resetPassword(UUID targetUserId, String newPassword, UUID currentAdminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        if (newPassword == null || newPassword.isBlank() || newPassword.trim().length() < 6) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        // The admin knows this password, so the user must replace it on next login
        user.issueTemporaryPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditor.record(currentAdminId, "USER_PASSWORD_RESET", "USER", user.getUserId(), Map.of("passwordReset", true));
    }

    /**
     * Resolve a single role code (ROLE_ADMIN, ROLE_LECTURER or ROLE_STUDENT) to its Role.
     * The {@code ROLE_} prefix is optional ({@code LECTURER} works); a blank code means ROLE_STUDENT.
     */
    private Role resolveRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return findDefaultRole();
        }
        return roleRepository.findByRoleCode(canonicalRoleCode(roleCode))
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROLE_NOT_FOUND));
    }

    private Role findDefaultRole() {
        return roleRepository.findByRoleCode("ROLE_STUDENT")
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROLE_NOT_FOUND));
    }

    private boolean isAdminRoleCode(String roleCode) {
        return "ROLE_ADMIN".equals(canonicalRoleCode(roleCode));
    }

    private String canonicalRoleCode(String roleCode) {
        String cleanCode = roleCode.trim().toUpperCase(Locale.ROOT);
        return cleanCode.startsWith("ROLE_") ? cleanCode : "ROLE_" + cleanCode;
    }
}
