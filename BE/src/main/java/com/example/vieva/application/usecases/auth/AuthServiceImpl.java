package com.example.vieva.application.usecases.auth;

import com.example.vieva.application.ports.input.LoginRequest;
import com.example.vieva.application.ports.input.RegisterRequest;
import com.example.vieva.application.ports.output.AuthResult;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Role defaultRole = roleRepository.findByRoleCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

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

        user.addRole(defaultRole, newUserId);

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getUserId(), savedUser.getEmail());

        return toAuthResult(savedUser, token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
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
                .expiresIn(86400)
                .userId(user.getUserId())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
