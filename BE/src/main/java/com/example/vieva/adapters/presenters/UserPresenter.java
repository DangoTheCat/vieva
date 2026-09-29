package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserPresenter {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
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
                        : Collections.emptySet())
                .createdAt(user.getCreatedAt())
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
}
