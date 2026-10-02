package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.ChangePasswordApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateProfileApiRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.UserDto;
import com.example.vieva.adapters.presenters.UserPresenter;
import com.example.vieva.application.ports.input.ChangePasswordRequest;
import com.example.vieva.application.ports.input.UpdateProfileRequest;
import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.usecases.user.AdminUserService;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminUserService adminUserService;
    private final UserPresenter userPresenter;

    /**
     * Returns the current authenticated user's profile.
     * Fetches directly from service to avoid Spring AOP self-invocation
     * which would bypass @PreAuthorize on getUserById.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        // Fetch fresh from service — avoids self-invocation AOP bypass
        User user = userService.getById(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(userPresenter.toDto(user));
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

    /**
     * @deprecated Prefer using /api/v1/admin/users for administrative user listing.
     * Retained for backward compatibility.
     */
    @Deprecated
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserDto>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .keyword(keyword)
                .role(role)
                .status(status)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResult<User> pagedUsers = adminUserService.getUsers(criteria);
        return ResponseEntity.ok(userPresenter.toPageResponse(pagedUsers));
    }

    /**
     * Access control: ADMIN can view any user; authenticated users can only view themselves.
     * The principal is checked directly via instanceof cast — the previous SpEL using
     * authentication.name == #id.toString() was always false and acted as dead code.
     * Also returns 404 for soft-deleted users (consistent with admin endpoint).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.example.vieva.domain.entities.User) and authentication.principal.userId == #id)")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = userService.getById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(userPresenter.toDto(user));
    }
}

