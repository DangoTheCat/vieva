package com.example.vieva.adapters.controllers;

import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.infrastructure.database.RoleJpaRepository;
import com.example.vieva.infrastructure.database.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleJpaRepository roleJpaRepository;

    @MockitoBean
    private UserJpaRepository userJpaRepository;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/users: unauthenticated request returns 401")
    void getAllUsers_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1003"))
                .andExpect(jsonPath("$.message").value("Unauthenticated"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/users: non-admin user request returns 403")
    void getAllUsers_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"))
                .andExpect(jsonPath("$.message").value("You do not have permission"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/users: admin user request returns 200")
    void getAllUsers_Admin_Returns200() throws Exception {
        User user1 = User.builder()
                .userId(UUID.randomUUID())
                .email("user1@example.com")
                .fullName("User One")
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(user1));

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"));
    }
}
