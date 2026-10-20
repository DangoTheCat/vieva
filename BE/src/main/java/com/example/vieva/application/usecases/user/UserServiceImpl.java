package com.example.vieva.application.usecases.user;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.user.ChangePasswordRequest;
import com.example.vieva.application.ports.input.user.UpdateProfileRequest;
import com.example.vieva.application.ports.input.user.UserResult;
import com.example.vieva.application.ports.input.user.UserService;
import com.example.vieva.application.ports.output.AvatarStoragePort;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    public Optional<UserResult> getById(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .map(UserResults::from);
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @UseCaseTransaction
    public UserResult updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);

        String targetFullName = user.getFullName();
        if (request.getFullName() != null) {
            String trimmedFullName = request.getFullName().trim();
            if (trimmedFullName.isEmpty()) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST);
            }
            targetFullName = trimmedFullName;
        }

        String targetPhone = user.getPhoneNumber();
        if (request.getPhoneNumber() != null) {
            String trimmedPhone = request.getPhoneNumber().trim();
            targetPhone = trimmedPhone.isEmpty() ? null : trimmedPhone;
        }

        user.updateProfile(targetFullName, targetPhone, Instant.now());

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

        User saved = userRepository.save(user);
        return UserResults.from(saved);
    }

    @Override
    @UseCaseTransaction
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INCORRECT_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @UseCaseTransaction
    public String uploadAvatar(UUID userId, byte[] imageBytes, String contentType) {
        User user = findActiveUser(userId);

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
    @UseCaseTransaction
    public void deleteAvatar(UUID userId) {
        User user = findActiveUser(userId);

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
