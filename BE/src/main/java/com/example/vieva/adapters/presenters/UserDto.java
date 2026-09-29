package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID userId;
    private String email;
    private String userCode;
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    private Set<String> roles;
    private Instant createdAt;
}
