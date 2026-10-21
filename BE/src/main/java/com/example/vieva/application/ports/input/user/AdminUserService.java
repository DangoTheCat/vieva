package com.example.vieva.application.ports.input.user;

import com.example.vieva.application.ports.input.common.PageResult;

import java.util.UUID;

public interface AdminUserService {

    PageResult<UserResult> getUsers(UserSearchCriteria criteria);

    UserResult getUserById(UUID userId);

    UserResult createUser(CreateUserByAdminRequest request, UUID currentAdminId);

    UserResult updateUser(UUID targetUserId, UpdateUserByAdminRequest request, UUID currentAdminId);

    void deleteUser(UUID targetUserId, UUID currentAdminId);

    void resetPassword(UUID targetUserId, String newPassword, UUID currentAdminId);
}
