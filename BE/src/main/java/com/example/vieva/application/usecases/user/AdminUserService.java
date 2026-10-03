package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.UserSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.User;

import java.util.UUID;

public interface AdminUserService {

    PagedResult<User> getUsers(UserSearchCriteria criteria);

    User getUserById(UUID userId);

    User createUser(CreateUserByAdminRequest request, UUID currentAdminId);

    User updateUser(UUID targetUserId, UpdateUserByAdminRequest request, UUID currentAdminId);

    void deleteUser(UUID targetUserId, UUID currentAdminId);

    void resetPassword(UUID targetUserId, String newPassword, UUID currentAdminId);
}
