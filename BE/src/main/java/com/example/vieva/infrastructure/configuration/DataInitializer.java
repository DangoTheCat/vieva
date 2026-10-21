package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.ports.input.user.EnsureDefaultRolesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final EnsureDefaultRolesUseCase ensureDefaultRolesUseCase;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> ensureDefaultRolesUseCase.ensureDefaultRoles();
    }
}
