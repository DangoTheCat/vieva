package com.example.vieva.presentation.controller;

import com.example.vieva.application.dto.ChangePasswordRequest;
import com.example.vieva.application.dto.MessageResponse;
import com.example.vieva.application.dto.UpdateProfileRequest;
import com.example.vieva.application.dto.UserDto;
import com.example.vieva.domain.entity.User;
import com.example.vieva.domain.service.UserService;
import com.example.vieva.infrastructure.exception.AppException;
import com.example.vieva.infrastructure.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return getUserById(currentUser.getUserId());
    }

    @PatchMapping("/me")
    public ResponseEntity<MessageResponse> updateCurrentUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        userService.updateProfile(currentUser.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        userService.changePassword(currentUser.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Password updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = userService.getById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(toDto(user));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(user.getUserRoles() != null
                        ? user.getUserRoles().stream()
                        .filter(ur -> ur.getRole() != null)
                        .map(ur -> ur.getRole().getRoleCode())
                        .collect(Collectors.toSet())
                        : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
