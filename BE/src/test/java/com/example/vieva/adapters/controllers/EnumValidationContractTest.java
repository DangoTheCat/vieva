package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.SubjectPresenter;
import com.example.vieva.adapters.web.GlobalExceptionHandler;
import com.example.vieva.application.ports.input.common.ActorView;
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

@ExtendWith(MockitoExtension.class)
class EnumValidationContractTest {

    @Mock
    private SubjectPresenter subjectPresenter;

    private AdminSubjectController adminSubjectController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        QuestionBankAuditor auditor = mock(QuestionBankAuditor.class);
        SubjectService subjectService = new SubjectServiceImpl(subjectRepository, auditor);
        adminSubjectController = new AdminSubjectController(subjectService, subjectPresenter);

        ActorView testAdmin = new ActorView(
                UUID.randomUUID(),
                "admin@vieva.edu.vn",
                java.util.Set.of("ROLE_ADMIN"),
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

        mockMvc = MockMvcBuilders.standaloneSetup(adminSubjectController)
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
}
