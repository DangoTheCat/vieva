package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.ChangePasswordRequest;
import com.example.vieva.application.ports.input.UpdateProfileRequest;
import com.example.vieva.application.ports.output.AvatarStoragePort;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AvatarStoragePort avatarStoragePort;

    @Override
    public Optional<User> getById(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted());
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getById(userId)
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

        if (request.getAvatarUrl() != null) {
            String trimmedAvatar = request.getAvatarUrl().trim();
            String oldAvatarUrl = user.getAvatarUrl();
            String newAvatar = trimmedAvatar.isEmpty() ? null : trimmedAvatar;
            if (oldAvatarUrl != null && !oldAvatarUrl.equals(newAvatar)) {
                try {
                    avatarStoragePort.deleteAvatar(oldAvatarUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar from Cloudinary: {}", e.getMessage());
                }
            }
            user.updateAvatar(newAvatar);
        }

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String uploadAvatar(UUID userId, byte[] imageBytes, String contentType) {
        User user = getById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = avatarStoragePort.uploadAvatar(userId, imageBytes, contentType);
        user.updateAvatar(newAvatarUrl);
        userRepository.save(user);

        // Xóa ảnh cũ trên Cloudinary để giải phóng bộ nhớ
        if (oldAvatarUrl != null && !oldAvatarUrl.equals(newAvatarUrl)) {
            try {
                avatarStoragePort.deleteAvatar(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar from Cloudinary: {}", e.getMessage());
            }
        }

        return newAvatarUrl;
    }

    @Override
    @Transactional
    public void deleteAvatar(UUID userId) {
        User user = getById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldAvatarUrl = user.getAvatarUrl();
        user.updateAvatar(null);
        userRepository.save(user);

        // Xóa ảnh cũ trên Cloudinary để giải phóng bộ nhớ
        if (oldAvatarUrl != null) {
            try {
                avatarStoragePort.deleteAvatar(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete avatar from Cloudinary: {}", e.getMessage());
            }
        }
    }
}
