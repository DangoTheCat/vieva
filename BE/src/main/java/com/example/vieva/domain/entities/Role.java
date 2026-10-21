package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Integer roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private Instant createdAt;
}
