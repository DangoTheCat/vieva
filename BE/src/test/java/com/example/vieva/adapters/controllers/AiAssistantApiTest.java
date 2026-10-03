package com.example.vieva.adapters.controllers;

import com.example.vieva.application.usecases.ai.AiChatService;
import com.example.vieva.application.usecases.ai.AiContextSnapshotService;
import com.example.vieva.domain.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI assistant endpoints through the real security chain: authentication, request validation and
 * response mapping.
 */
@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.datasource.password=testpassword",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AiAssistantApiTest {

    @Autowired private MockMvc mockMvc;

    // JPA is disabled in this slice: every Spring Data repository is mocked.
    @MockitoBean private com.example.vieva.infrastructure.database.RoleJpaRepository roleJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.UserJpaRepository userJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.AuditEventJpaRepository auditEventJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.SubjectJpaRepository subjectJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.LecturerSubjectJpaRepository lecturerSubjectJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.CourseDocumentJpaRepository courseDocumentJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.DocumentChunkJpaRepository documentChunkJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.QuestionJpaRepository questionJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.QuestionVersionJpaRepository questionVersionJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.QuestionSourceJpaRepository questionSourceJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.RubricJpaRepository rubricJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.RubricCriterionJpaRepository rubricCriterionJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.TopicJpaRepository topicJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.AiRuleJpaRepository aiRuleJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.QuestionGenerationRequestJpaRepository generationJpaRepository;

    @MockitoBean private AiChatService aiChatService;
    @MockitoBean private AiContextSnapshotService snapshotService;

    private final User student = User.builder().userId(UUID.randomUUID()).email("sv@fpt.edu.vn").build();

    private RequestPostProcessor as(User user, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    @DisplayName("401 without authentication")
    void unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/ai/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Xin chào\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1003"));
        verify(aiChatService, never()).chat(any());
    }

    @Test
    @DisplayName("400 when the message is blank")
    void blankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/ai/assistant/chat").with(as(student, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(aiChatService, never()).chat(any());
    }

    @Test
    @DisplayName("200 chat for a student: message and user id reach the use case")
    void chat() throws Exception {
        Instant generatedAt = Instant.parse("2026-10-03T10:00:00Z");
        when(aiChatService.chat(any())).thenReturn(new AiChatService.ChatReply("Bạn có 60 giây để kết nối lại.", generatedAt));

        mockMvc.perform(post("/api/v1/ai/assistant/chat").with(as(student, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Mất mạng thì sao?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Bạn có 60 giây để kết nối lại."))
                .andExpect(jsonPath("$.snapshotGeneratedAt").value("2026-10-03T10:00:00Z"));
        verify(aiChatService).chat(argThat(c -> c.userId().equals(student.getUserId())
                && c.message().equals("Mất mạng thì sao?")));
    }

    @Test
    @DisplayName("200 context snapshot for any signed-in user")
    void contextSnapshot() throws Exception {
        when(snapshotService.current()).thenReturn(new AiContextSnapshotService.CachedSnapshot(
                "=== SNAPSHOT ===", Instant.parse("2026-10-03T10:00:00Z")));

        mockMvc.perform(get("/api/v1/ai/context-snapshot").with(as(student, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("=== SNAPSHOT ==="))
                .andExpect(jsonPath("$.generatedAt").value("2026-10-03T10:00:00Z"));
    }
}
