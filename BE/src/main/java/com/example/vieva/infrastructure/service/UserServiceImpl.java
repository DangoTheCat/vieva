package com.example.vieva.infrastructure.service;

import com.example.vieva.application.dto.ChangePasswordRequest;
import com.example.vieva.application.dto.UpdateProfileRequest;
import com.example.vieva.domain.entity.User;
import com.example.vieva.domain.repository.UserRepository;
import com.example.vieva.domain.service.UserService;
import com.example.vieva.infrastructure.exception.AppException;
import com.example.vieva.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    @Lazy
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> getById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getByUserCode(String userCode) {
        return userRepository.findByUserCode(userCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            String trimmedFullName = request.getFullName().trim();
            if (trimmedFullName.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            user.setFullName(trimmedFullName);
        }

        if (request.getPhoneNumber() != null) {
            String trimmedPhone = request.getPhoneNumber().trim();
            user.setPhoneNumber(trimmedPhone.isEmpty() ? null : trimmedPhone);
        }

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}
