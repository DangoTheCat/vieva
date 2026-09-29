package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.ChangePasswordApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateProfileApiRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.UserDto;
import com.example.vieva.adapters.presenters.UserPresenter;
import com.example.vieva.application.ports.input.ChangePasswordRequest;
import com.example.vieva.application.ports.input.UpdateProfileRequest;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPresenter userPresenter;

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
            @Valid @RequestBody UpdateProfileApiRequest apiRequest) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName(apiRequest.getFullName())
                .phoneNumber(apiRequest.getPhoneNumber())
                .build();

        userService.updateProfile(currentUser.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordApiRequest apiRequest) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword(apiRequest.getOldPassword())
                .newPassword(apiRequest.getNewPassword())
                .build();

        userService.changePassword(currentUser.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Password updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(userPresenter.toDtoList(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.example.vieva.domain.entities.User) and authentication.principal.userId == #id) or authentication.name == #id.toString()")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = userService.getById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(userPresenter.toDto(user));
    }
}
