package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.application.usecases.generation.QuestionGenerationService;
import com.example.vieva.application.usecases.importing.QuestionImportService;
import com.example.vieva.application.usecases.question.QuestionAuthoringService;
import com.example.vieva.application.usecases.question.QuestionReviewService;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Group-1 endpoints through the real security chain: role rules, course-scoped @PreAuthorize,
 * request validation and business error mapping (status + symbolic code + field errors).
 */
@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.datasource.password=testpassword",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class LecturerQuestionBankApiTest {

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

    @MockitoBean private UserService userService;
    @MockitoBean private LecturerSubjectRepository lecturerSubjectRepository;
    @MockitoBean private QuestionRepository questionRepository;
    @MockitoBean private QuestionVersionRepository questionVersionRepository;
    @MockitoBean private QuestionAuthoringService authoringService;
    @MockitoBean private QuestionReviewService reviewService;
    @MockitoBean private QuestionGenerationService generationService;
    @MockitoBean private QuestionImportService importService;
    @MockitoBean private DocumentIndexingService documentIndexingService;

    private final UUID subjectId = UUID.randomUUID();
    private User lecturer;

    private static final String VALID_QUESTION = """
            {"content":"Trình bày ACID?","expectedAnswer":"Atomicity...","bloomLevel":"UNDERSTAND",
             "rubric":{"criteria":[{"name":"Đủ ý","description":"0-10","maxScore":10}]}}
            """;

    @BeforeEach
    void setUp() {
        lecturer = User.builder().userId(UUID.randomUUID()).email("gv@fpt.edu.vn").build();
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer.getUserId(), subjectId)).thenReturn(true);
    }

    private RequestPostProcessor as(User user, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
    }

    private QuestionVersionView draftView() {
        Question question = Question.newDraftOwner(subjectId, null, lecturer.getUserId());
        QuestionVersion version = QuestionVersion.newDraft(question.getQuestionId(), 1, "Trình bày ACID?", "Atomicity...",
                BloomLevel.UNDERSTAND, QuestionGenerationMode.MANUAL, lecturer.getUserId());
        version.setVersion(0L);
        return QuestionVersionView.builder().question(question).version(version).criteria(List.of()).sources(List.of()).build();
    }

    @Test
    @DisplayName("401 without authentication")
    void unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_QUESTION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1003"));
    }

    @Test
    @DisplayName("403 for a student (role rule on /api/v1/lecturer/**)")
    void studentForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId).with(as(lecturer, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_QUESTION))
                .andExpect(status().isForbidden());
        verify(authoringService, never()).createManualQuestion(any(), any(), any());
    }

    @Test
    @DisplayName("BR-06: 403 for a lecturer not assigned to the subject")
    void lecturerNotAssigned() throws Exception {
        UUID otherSubject = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", otherSubject).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_QUESTION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"));
        verify(authoringService, never()).createManualQuestion(any(), any(), any());
    }

    @Test
    @DisplayName("201 for an assigned lecturer; the response carries the lock token")
    void createManualQuestion() throws Exception {
        when(authoringService.createManualQuestion(eq(subjectId), any(), eq(lecturer.getUserId()))).thenReturn(draftView());
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_QUESTION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.bloomLevelLabel").value("Hiểu"))
                .andExpect(jsonPath("$.lockVersion").value(0));
    }

    @Test
    @DisplayName("400 with every invalid field listed")
    void validationErrors() throws Exception {
        String body = """
                {"content":" ","expectedAnswer":"x","bloomLevel":"APPLY","rubric":{"criteria":[]}}
                """;
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("400 for a Bloom level outside the 6 levels")
    void unknownBloomLevel() throws Exception {
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUESTION.replace("UNDERSTAND", "VAN_DUNG_CAO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("422 with field errors when the rubric total does not match")
    void businessFieldErrors() throws Exception {
        when(authoringService.createManualQuestion(any(), any(), any())).thenThrow(AppException.ofViolations(List.of(
                FieldViolation.of("rubric.totalScore", ErrorCode.RUBRIC_SCORE_MISMATCH, "Total 12 must equal 10"))));
        mockMvc.perform(post("/api/v1/lecturer/subjects/{id}/questions", subjectId).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_QUESTION))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("RUBRIC_SCORE_MISMATCH"))
                .andExpect(jsonPath("$.errors[0].field").value("rubric.totalScore"));
    }

    @Test
    @DisplayName("approve: 409 VERSION_NOT_DRAFT, 422 BLOOM_NOT_CONFIRMED, 409 CONCURRENT_MODIFICATION")
    void approveErrorMapping() throws Exception {
        QuestionVersionView view = draftView();
        UUID versionId = view.version().getQuestionVersionId();
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(view.version()));
        when(questionRepository.findById(view.question().getQuestionId())).thenReturn(Optional.of(view.question()));

        when(reviewService.approve(eq(versionId), any(), any())).thenThrow(new AppException(ErrorCode.VERSION_NOT_DRAFT));
        mockMvc.perform(post("/api/v1/lecturer/question-versions/{id}/approve", versionId).with(as(lecturer, "LECTURER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("VERSION_NOT_DRAFT"));

        when(reviewService.approve(eq(versionId), any(), any())).thenThrow(AppException.ofViolations(List.of(
                FieldViolation.of("bloomConfirmed", ErrorCode.BLOOM_NOT_CONFIRMED, "confirm"),
                FieldViolation.of("sources", ErrorCode.SOURCE_REQUIRED, "source"))));
        mockMvc.perform(post("/api/v1/lecturer/question-versions/{id}/approve", versionId).with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BLOOM_NOT_CONFIRMED"))
                .andExpect(jsonPath("$.errors[1].error").value("SOURCE_REQUIRED"));

        when(reviewService.approve(eq(versionId), any(), any())).thenThrow(new AppException(ErrorCode.CONCURRENT_MODIFICATION));
        mockMvc.perform(post("/api/v1/lecturer/question-versions/{id}/approve", versionId).with(as(lecturer, "LECTURER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("approve a version of another subject → 403 before reaching the use case")
    void approveForeignVersion() throws Exception {
        Question foreign = Question.newDraftOwner(UUID.randomUUID(), null, UUID.randomUUID());
        QuestionVersion version = QuestionVersion.newDraft(foreign.getQuestionId(), 1, "c", "a", BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, UUID.randomUUID());
        when(questionVersionRepository.findById(version.getQuestionVersionId())).thenReturn(Optional.of(version));
        when(questionRepository.findById(foreign.getQuestionId())).thenReturn(Optional.of(foreign));

        mockMvc.perform(post("/api/v1/lecturer/question-versions/{id}/approve", version.getQuestionVersionId())
                        .with(as(lecturer, "LECTURER")))
                .andExpect(status().isForbidden());
        verify(reviewService, never()).approve(any(), any(), any());
    }

    @Test
    @DisplayName("upload answers 202 Accepted with the UPLOADED document")
    void uploadAccepted() throws Exception {
        CourseDocument document = CourseDocument.builder().documentId(UUID.randomUUID()).subjectId(subjectId)
                .fileName("csdl.pdf").indexingStatus(DocumentIndexingStatus.UPLOADED).indexAttempts(0).build();
        when(documentIndexingService.uploadDocument(eq(subjectId), eq("csdl.pdf"), any(), eq(lecturer.getUserId())))
                .thenReturn(document);
        mockMvc.perform(multipart("/api/v1/lecturer/subjects/{id}/documents", subjectId)
                        .file(new MockMultipartFile("file", "csdl.pdf", "application/pdf", "%PDF".getBytes()))
                        .with(as(lecturer, "LECTURER")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.indexingStatus").value("UPLOADED"));

        when(documentIndexingService.uploadDocument(any(), any(), any(), any()))
                .thenThrow(new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE));
        mockMvc.perform(multipart("/api/v1/lecturer/subjects/{id}/documents", subjectId)
                        .file(new MockMultipartFile("file", "virus.exe", "application/pdf", "MZ".getBytes()))
                        .with(as(lecturer, "LECTURER")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    @DisplayName("generation request is authorized on the subjectId of the body")
    void generationAuthorization() throws Exception {
        String body = """
                {"subjectId":"%s","documentIds":["%s"],"totalQuestions":2,"bloomDistribution":{"APPLY":1,"CREATE":1}}
                """;
        mockMvc.perform(post("/api/v1/lecturer/question-generation-requests").with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isForbidden());

        when(generationService.generate(any(), any())).thenThrow(new AppException(ErrorCode.DOCUMENT_NOT_READY));
        mockMvc.perform(post("/api/v1/lecturer/question-generation-requests").with(as(lecturer, "LECTURER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(subjectId, UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_READY"));
        verify(generationService).generate(any(), eq(lecturer.getUserId()));
    }

    @Test
    void importTemplateDownload() throws Exception {
        when(importService.template("csv")).thenReturn("question_ref\n".getBytes());
        mockMvc.perform(get("/api/v1/lecturer/import-templates/questions").param("format", "csv")
                        .with(as(lecturer, "LECTURER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("question-import-template.csv")));
    }
}
