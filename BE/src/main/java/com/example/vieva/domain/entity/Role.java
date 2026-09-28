package com.example.vieva.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Integer roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private Instant createdAt;
}
