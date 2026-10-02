package com.example.vieva;

import com.example.vieva.infrastructure.database.RoleJpaRepository;
import com.example.vieva.infrastructure.database.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.datasource.password=testpassword",
        "spring.flyway.enabled=false"
})
class VievaApplicationTests {

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
    private com.example.vieva.infrastructure.database.QuestionGenerationRequestJpaRepository questionGenerationRequestJpaRepository;

	@Test
	void contextLoads() {
	}
}
