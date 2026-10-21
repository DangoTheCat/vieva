package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.CourseDocumentPresenter;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.adapters.presenters.SubjectPresenter;
import com.example.vieva.adapters.web.GlobalExceptionHandler;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.document.DocumentService;
import com.example.vieva.application.ports.input.generation.QuestionGenCoordinator;
import com.example.vieva.application.ports.input.question.QuestionAuthoringService;
import com.example.vieva.application.ports.input.question.QuestionBankService;
import com.example.vieva.application.ports.input.question.QuestionReviewService;
import com.example.vieva.application.ports.input.subject.SubjectService;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
import com.example.vieva.application.usecases.subject.SubjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionViewAssemblerPort;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.ports.input.access.SubjectHeadService;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.generation.QuestionGenCoordinatorImpl;
import com.example.vieva.application.usecases.question.QuestionAccessLoader;
import com.example.vieva.application.usecases.question.QuestionAuthoringServiceImpl;
import com.example.vieva.application.usecases.question.QuestionBankServiceImpl;
import com.example.vieva.application.usecases.question.QuestionReviewServiceImpl;
import com.example.vieva.application.usecases.question.ReviewNotifications;

@ExtendWith(MockitoExtension.class)
class EnumValidationContractTest {

    @Mock
    private SubjectPresenter subjectPresenter;

    private AdminSubjectController adminSubjectController;
    private LecturerStudioController studioController;
    private LecturerQuestionController questionController;
    private LecturerQuestionVersionController versionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        QuestionBankAuditor auditor = mock(QuestionBankAuditor.class);
        SubjectService subjectService = new SubjectServiceImpl(subjectRepository, auditor);
        adminSubjectController = new AdminSubjectController(subjectService, subjectPresenter);

        DocumentService docService = mock(DocumentService.class);
        QuestionBankPresenter presenter = mock(QuestionBankPresenter.class);
        CourseDocumentPresenter documentPresenter = mock(CourseDocumentPresenter.class);

        QuestionRepository questionRepository = mock(QuestionRepository.class);
        QuestionVersionRepository questionVersionRepository = mock(QuestionVersionRepository.class);
        RubricRepository rubricRepository = mock(RubricRepository.class);
        RubricCriterionRepository rubricCriterionRepository = mock(RubricCriterionRepository.class);
        QuestionSourceRepository questionSourceRepository = mock(QuestionSourceRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        CourseDocumentRepository courseDocumentRepository = mock(CourseDocumentRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        QuestionGenerationRequestRepository genRequestRepository = mock(QuestionGenerationRequestRepository.class);
        QuestionGenerationPort questionGenerator = mock(QuestionGenerationPort.class);
        TransactionRunnerPort transactionRunner = mock(TransactionRunnerPort.class);
        SubjectAccessGuard subjectAccessGuard = mock(SubjectAccessGuard.class);
        QuestionAccessLoader accessLoader = mock(QuestionAccessLoader.class);
        QuestionDraftWriter draftWriter = mock(QuestionDraftWriter.class);
        QuestionVersionViewAssemblerPort viewAssembler = mock(QuestionVersionViewAssemblerPort.class);
        SubjectHeadService heads = mock(SubjectHeadService.class);
        ReviewNotifications notifications = mock(ReviewNotifications.class);
        RagSettings ragSettings = mock(RagSettings.class);

        QuestionBankService bankService = new QuestionBankServiceImpl(
                questionRepository, questionVersionRepository, accessLoader, viewAssembler, auditor);
        QuestionReviewService reviews = new QuestionReviewServiceImpl(
                questionRepository, questionVersionRepository, rubricRepository, rubricCriterionRepository,
                questionSourceRepository, documentChunkRepository, courseDocumentRepository, accessLoader,
                viewAssembler, auditor, heads, notifications, draftWriter);
        QuestionAuthoringService authoringService = new QuestionAuthoringServiceImpl(
                questionRepository, questionVersionRepository, rubricRepository, rubricCriterionRepository,
                questionSourceRepository, topicRepository, subjectAccessGuard, accessLoader, draftWriter,
                viewAssembler, auditor);
        QuestionGenCoordinator ragCoord = new QuestionGenCoordinatorImpl(
                genRequestRepository, questionVersionRepository, rubricRepository, rubricCriterionRepository,
                questionSourceRepository, courseDocumentRepository, documentChunkRepository, subjectRepository,
                topicRepository, questionGenerator, transactionRunner, subjectAccessGuard, accessLoader,
                draftWriter, viewAssembler, auditor, ragSettings);

        studioController = new LecturerStudioController(docService, ragCoord, reviews, presenter, documentPresenter);
        questionController = new LecturerQuestionController(authoringService, bankService, presenter);
        versionController = new LecturerQuestionVersionController(reviews, authoringService, ragCoord, presenter);

        ActorView testAdmin = new ActorView(
                UUID.randomUUID(),
                "admin@vieva.edu.vn",
                java.util.Set.of("ROLE_ADMIN", "ROLE_LECTURER"),
                false
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testAdmin;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(
                        adminSubjectController,
                        studioController,
                        questionController,
                        versionController)
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorTranslatorImpl()))
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("Invalid enum in request body returns HTTP 400 with 'Malformed request body'")
    void invalidEnumInRequestBodyReturnsMalformedRequestBody() throws Exception {
        String invalidBody = """
                {
                  "subjectCode": "PRJ301",
                  "subjectName": "Java Web",
                  "status": "INVALID_STATUS"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    @DisplayName("Invalid enum in query param returns HTTP 400 with 'Invalid value for parameter <name>'")
    void invalidEnumInQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subjects")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    @DisplayName("Invalid BloomLevel in studio generate request body returns HTTP 400 with 'Malformed request body'")
    void invalidBloomLevelInStudioGenerateRequestBodyReturnsMalformedRequestBody() throws Exception {
        String invalidBody = """
                {
                  "topicId": "00000000-0000-0000-0000-000000000001",
                  "difficulty": "INVALID_BLOOM",
                  "documentIds": []
                }
                """;

        mockMvc.perform(post("/api/v1/lecturer/studio/questions/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    @DisplayName("Invalid bloomLevel in question search query param returns HTTP 400 with 'Invalid value for parameter bloomLevel'")
    void invalidBloomLevelInQuestionSearchQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/subjects/00000000-0000-0000-0000-000000000001/questions")
                        .param("bloomLevel", "INVALID_BLOOM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'bloomLevel'"));
    }

    @Test
    @DisplayName("Invalid status in question search query param returns HTTP 400 with 'Invalid value for parameter status'")
    void invalidStatusInQuestionSearchQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/subjects/00000000-0000-0000-0000-000000000001/questions")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    @DisplayName("Invalid status in question version search query param returns HTTP 400 with 'Invalid value for parameter status'")
    void invalidStatusInQuestionVersionSearchQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/question-versions")
                        .param("subjectId", "00000000-0000-0000-0000-000000000001")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    @DisplayName("Invalid origin in question version search query param returns HTTP 400 with 'Invalid value for parameter origin'")
    void invalidOriginInQuestionVersionSearchQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/question-versions")
                        .param("subjectId", "00000000-0000-0000-0000-000000000001")
                        .param("origin", "INVALID_ORIGIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'origin'"));
    }

    @Test
    @DisplayName("Invalid bloomLevel in question version search query param returns HTTP 400 with 'Invalid value for parameter bloomLevel'")
    void invalidBloomLevelInQuestionVersionSearchQueryParamReturnsInvalidValueForParameter() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/question-versions")
                        .param("subjectId", "00000000-0000-0000-0000-000000000001")
                        .param("bloomLevel", "INVALID_BLOOM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'bloomLevel'"));
    }

    @Test
    @DisplayName("Invalid bloomLevel and stale expectedVersion in confirmBloom returns HTTP 400 with 'Malformed request body'")
    void invalidBloomLevelAndStaleExpectedVersionInConfirmBloomReturnsMalformedRequestBody() throws Exception {
        UUID versionId = UUID.randomUUID();
        String invalidBody = """
                {
                  "bloomLevel": "INVALID_BLOOM",
                  "expectedVersion": 999
                }
                """;

        mockMvc.perform(post("/api/v1/lecturer/question-versions/{versionId}/confirm-bloom", versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}
