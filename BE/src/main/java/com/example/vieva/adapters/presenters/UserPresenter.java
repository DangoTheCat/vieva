package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class UserPresenter {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        Role role = user.getRole();
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .role(role != null ? role.getRoleCode() : null)
                .roles(role != null ? Set.of(role.getRoleCode()) : Collections.emptySet())
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public PageResponse<UserDto> toPageResponse(PagedResult<User> pagedResult) {
        return PageResponse.from(pagedResult, this::toDto);
    }
}
