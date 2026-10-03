package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.usecases.user.AdminUserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.datasource.password=testpassword",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleJpaRepository roleJpaRepository;

    @MockitoBean
    private UserJpaRepository userJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.AuditEventJpaRepository auditEventJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.SubjectJpaRepository subjectJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.LecturerSubjectJpaRepository lecturerSubjectJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.CourseDocumentJpaRepository courseDocumentJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.DocumentChunkJpaRepository documentChunkJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.QuestionJpaRepository questionJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.QuestionVersionJpaRepository questionVersionJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.QuestionSourceJpaRepository questionSourceJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.RubricJpaRepository rubricJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.RubricCriterionJpaRepository rubricCriterionJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.TopicJpaRepository topicJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.AiRuleJpaRepository aiRuleJpaRepository;

    @MockitoBean
    private com.example.vieva.infrastructure.database.QuestionGenerationRequestJpaRepository questionGenerationRequestJpaRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AdminUserService adminUserService;

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
    @DisplayName("GET /api/v1/users: admin user request returns 200 and user list")
    void getAllUsers_Admin_Returns200() throws Exception {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@vieva.edu.vn")
                .fullName("System Admin")
                .build();
        PagedResult<User> pagedResult = PagedResult.of(List.of(user), 0, 20, 1);
        when(adminUserService.getUsers(any())).thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("admin@vieva.edu.vn"));
    }
}
