package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.CreateUserByAdminApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateUserByAdminApiRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.UserDto;
import com.example.vieva.adapters.presenters.UserPresenter;
import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.usecases.user.AdminUserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserPresenter userPresenter;

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = adminUserService.getUserById(id);
        return ResponseEntity.ok(userPresenter.toDto(user));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody CreateUserByAdminApiRequest apiRequest) {

        UUID currentAdminId = resolveCurrentAdminId(currentAdmin);

        CreateUserByAdminRequest request = CreateUserByAdminRequest.builder()
                .email(apiRequest.getEmail())
                .password(apiRequest.getPassword())
                .fullName(apiRequest.getFullName())
                .phoneNumber(apiRequest.getPhoneNumber())
                .userCode(apiRequest.getUserCode())
                .status(apiRequest.getStatus())
                .roleCode(resolveRoleCode(apiRequest.getRoleCode(), apiRequest.getRoleCodes()))
                .build();

        User createdUser = adminUserService.createUser(request, currentAdminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userPresenter.toDto(createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody UpdateUserByAdminApiRequest apiRequest) {

        UUID currentAdminId = resolveCurrentAdminId(currentAdmin);

        UpdateUserByAdminRequest request = UpdateUserByAdminRequest.builder()
                .fullName(apiRequest.getFullName())
                .phoneNumber(apiRequest.getPhoneNumber())
                .status(apiRequest.getStatus())
                .roleCode(resolveRoleCode(apiRequest.getRoleCode(), apiRequest.getRoleCodes()))
                .build();

        User updatedUser = adminUserService.updateUser(id, request, currentAdminId);
        return ResponseEntity.ok(userPresenter.toDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {

        UUID currentAdminId = resolveCurrentAdminId(currentAdmin);
        adminUserService.deleteUser(id, currentAdminId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody com.example.vieva.adapters.controllers.request.AdminResetPasswordApiRequest request) {

        UUID currentAdminId = resolveCurrentAdminId(currentAdmin);
        adminUserService.resetPassword(id, request.getNewPassword(), currentAdminId);
        return ResponseEntity.ok(new MessageResponse("User password has been reset successfully"));
    }

    /**
     * Accepts the single {@code roleCode} field, falling back to the deprecated {@code roleCodes}
     * set for existing clients. An account has exactly one role, so more than one code is rejected.
     */
    private String resolveRoleCode(String roleCode, Set<String> legacyRoleCodes) {
        if (StringUtils.hasText(roleCode)) {
            return roleCode.trim();
        }
        if (legacyRoleCodes == null) {
            return null;
        }
        List<String> codes = legacyRoleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (codes.size() > 1) {
            throw new AppException(ErrorCode.MULTIPLE_ROLES_NOT_ALLOWED);
        }
        return codes.isEmpty() ? null : codes.get(0);
    }

    private UUID resolveCurrentAdminId(User currentAdmin) {
        if (currentAdmin == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return currentAdmin.getUserId();
    }
}
