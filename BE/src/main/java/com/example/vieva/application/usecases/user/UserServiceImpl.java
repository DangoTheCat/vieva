package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.ChangePasswordRequest;
import com.example.vieva.application.ports.input.UpdateProfileRequest;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    public Optional<User> getById(UUID userId) {
        return userRepository.findById(userId);
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
        user.setPasswordChangedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}
