package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.domain.entities.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            initRoleIfNotExist("ROLE_USER", "Standard User", "Quyền người dùng thông thường");
            initRoleIfNotExist("ROLE_ADMIN", "Administrator", "Quản trị viên hệ thống");
        };
    }

    private void initRoleIfNotExist(String roleCode, String roleName, String description) {
        if (roleRepository.findByRoleCode(roleCode).isEmpty()) {
            Role role = Role.builder()
                    .roleCode(roleCode)
                    .roleName(roleName)
                    .description(description)
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(role);
            log.info("Initialized default role: {}", roleCode);
        }
    }
}
