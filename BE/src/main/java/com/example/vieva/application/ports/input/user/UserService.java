package com.example.vieva.application.ports.input.user;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Optional<UserResult> getById(UUID userId);
    UserResult updateProfile(UUID userId, UpdateProfileRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
    String uploadAvatar(UUID userId, byte[] imageBytes, String contentType);
    void deleteAvatar(UUID userId);
}
