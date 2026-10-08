package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PageResponse<UserDto> toPageResponse(PagedResult<User> pagedResult) {
        if (pagedResult == null) {
            return null;
        }
        return PageResponse.<UserDto>builder()
                .content(toDtoList(pagedResult.getContent()))
                .page(pagedResult.getPage())
                .size(pagedResult.getSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .isFirst(pagedResult.isFirst())
                .isLast(pagedResult.isLast())
                .build();
    }
}
