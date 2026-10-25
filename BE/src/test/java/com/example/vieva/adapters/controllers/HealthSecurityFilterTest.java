package com.example.vieva.adapters.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.datasource.password=testpassword"
})
@AutoConfigureMockMvc
class HealthSecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private com.example.vieva.application.ports.output.NotificationRepository notificationRepository;
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
    @MockitoBean private com.example.vieva.infrastructure.database.QuestionGenerationRequestJpaRepository questionGenerationRequestJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamAttemptJpaRepository examAttemptJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamTurnJpaRepository examTurnJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamScoreJpaRepository examScoreJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.SpeechSettingsJpaRepository speechSettingsJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.RubricTemplateJpaRepository rubricTemplateJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamJpaRepository examJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamQuestionJpaRepository examQuestionJpaRepository;
    @MockitoBean private com.example.vieva.infrastructure.database.ExamAttemptQuestionSnapshotJpaRepository examAttemptQuestionSnapshotJpaRepository;

    @Test
    @DisplayName("Unauthenticated request to /api/v1/health passes security filter chain and returns 200 UP")
    void health_unauthenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
