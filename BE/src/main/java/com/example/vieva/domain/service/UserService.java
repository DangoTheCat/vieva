package com.example.vieva.domain.service;

import com.example.vieva.application.dto.ChangePasswordRequest;
import com.example.vieva.application.dto.UpdateProfileRequest;
import com.example.vieva.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Optional<User> getById(UUID userId);
    Optional<User> getByEmail(String email);
    Optional<User> getByUserCode(String userCode);
    List<User> getAllUsers();
    User updateProfile(UUID userId, UpdateProfileRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
}
