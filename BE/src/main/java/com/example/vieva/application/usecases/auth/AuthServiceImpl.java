package com.example.vieva.application.usecases.auth;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.auth.AuthResult;
import com.example.vieva.application.ports.input.auth.AuthService;
import com.example.vieva.application.ports.input.auth.LoginRequest;
import com.example.vieva.application.ports.input.auth.RegisterRequest;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final com.example.vieva.application.settings.AuthSettings authSettings;

    @Override
    @UseCaseTransaction
    public AuthResult register(RegisterRequest request) {
        String email = new Email(request.getEmail()).getValue();

        if (!authSettings.isRegistrationAllowed(email)) {
            throw new ApplicationException(ErrorCode.REGISTRATION_DOMAIN_NOT_ALLOWED);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.USER_EXISTED);
        }

        // Self-registered accounts are students
        Role defaultRole = roleRepository.findByRoleCode("ROLE_STUDENT")
                .orElseThrow(() -> new ApplicationException(ErrorCode.ROLE_NOT_FOUND));

        UUID newUserId = UUID.randomUUID();
        String userCode = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = User.builder()
                .userId(newUserId)
                .email(email)
                .userCode(userCode)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user.assignRole(defaultRole, newUserId);

        final User savedUser;
        try {
            // Flush forces the INSERT now so a concurrent duplicate surfaces here (unique index)
            savedUser = userRepository.saveAndFlush(user);
        } catch (DuplicateKeyException e) {
            // Concurrent registration with the same email won the race (unique index)
            throw new ApplicationException(ErrorCode.USER_EXISTED);
        }
        String token = tokenProvider.generateToken(savedUser.getUserId(), savedUser.getEmail());

        return toAuthResult(savedUser, token);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String email = new Email(request.getEmail()).getValue();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHENTICATED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.USER_INACTIVE);
        }

        String token = tokenProvider.generateToken(user.getUserId(), user.getEmail());

        return toAuthResult(user, token);
    }

    private AuthResult toAuthResult(User user, String token) {
        Set<String> roles = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return AuthResult.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationInSeconds())
                .userId(user.getUserId())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
