package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.ExamAttemptPresenter;
import com.example.vieva.adapters.web.GlobalExceptionHandler;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.*;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentExamAttemptControllerTest {

    @Mock
    private StartExamUseCase startExamUseCase;
    @Mock
    private StartExamSessionUseCase startExamSessionUseCase;
    @Mock
    private ManageExamConnectionUseCase manageExamConnectionUseCase;
    @Mock
    private GetExamAttemptUseCase getExamAttemptUseCase;
    @Mock
    private SubmitTurnAnswerUseCase submitTurnAnswerUseCase;

    private MockMvc mockMvc;
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ExamAttemptPresenter presenter = new ExamAttemptPresenter();
        StudentExamAttemptController controller = new StudentExamAttemptController(
                startExamUseCase,
                startExamSessionUseCase,
                manageExamConnectionUseCase,
                getExamAttemptUseCase,
                submitTurnAnswerUseCase,
                presenter
        );

        ActorView studentActor = new ActorView(
                studentId,
                "student@vieva.edu.vn",
                Set.of("ROLE_STUDENT"),
                false
        );

        HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return studentActor;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorTranslatorImpl()))
                .setCustomArgumentResolvers(authResolver)
                .build();
    }

    private ExamAttemptView buildView(String status) {
        Instant now = Instant.now();
        return new ExamAttemptView(
                attemptId,
                studentId,
                subjectId,
                status,
                0,
                5,
                BigDecimal.ZERO,
                null,
                now,
                null,
                now,
                850,
                now.plusSeconds(120),
                UUID.randomUUID(),
                "Sample question?",
                "http://audio.url"
        );
    }

    @Test
    @DisplayName("POST /attempts initializes exam attempt in READY state")
    void startExamEndpoint() throws Exception {
        when(startExamUseCase.startExam(eq(studentId), eq(subjectId))).thenReturn(buildView("READY"));

        mockMvc.perform(post("/api/v1/student/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": "%s"
                                }
                                """.formatted(subjectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("POST /attempts/{id}/start starts session with deviceReady")
    void startSessionEndpoint() throws Exception {
        when(startExamSessionUseCase.startSession(eq(attemptId), eq(studentId), eq(true)))
                .thenReturn(buildView("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/start", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceReady": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(startExamSessionUseCase).startSession(eq(attemptId), eq(studentId), eq(true));
    }

    @Test
    @DisplayName("POST /attempts/{id}/start returns 400 when device check fails")
    void startSessionEndpointDeviceNotReady() throws Exception {
        when(startExamSessionUseCase.startSession(eq(attemptId), eq(studentId), eq(false)))
                .thenThrow(new com.example.vieva.domain.exception.DomainException(
                        com.example.vieva.domain.exception.DomainErrorCode.INVALID_REQUEST,
                        "Cannot start exam attempt: device check not passed"
                ));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/start", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceReady": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot start exam attempt: device check not passed"));
    }

    @Test
    @DisplayName("POST /attempts/{id}/disconnect handles disconnect and returns timing fields")
    void disconnectEndpoint() throws Exception {
        when(manageExamConnectionUseCase.disconnect(eq(attemptId), eq(studentId)))
                .thenReturn(buildView("RECONNECTING"));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/disconnect", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONNECTING"))
                .andExpect(jsonPath("$.remainingSeconds").value(850))
                .andExpect(jsonPath("$.graceDeadline").exists());

        verify(manageExamConnectionUseCase).disconnect(eq(attemptId), eq(studentId));
    }

    @Test
    @DisplayName("POST /attempts/{id}/reconnect handles reconnect")
    void reconnectEndpoint() throws Exception {
        when(manageExamConnectionUseCase.reconnect(eq(attemptId), eq(studentId)))
                .thenReturn(buildView("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/reconnect", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(manageExamConnectionUseCase).reconnect(eq(attemptId), eq(studentId));
    }

    @Test
    @DisplayName("POST /attempts/{id}/reconnect returns 400 when grace period has expired")
    void reconnectEndpointGraceExpired() throws Exception {
        when(manageExamConnectionUseCase.reconnect(eq(attemptId), eq(studentId)))
                .thenThrow(new com.example.vieva.domain.exception.DomainException(
                        com.example.vieva.domain.exception.DomainErrorCode.INVALID_REQUEST,
                        "Cannot reconnect: grace period has expired"
                ));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/reconnect", attemptId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot reconnect: grace period has expired"));
    }

    @Test
    @DisplayName("POST /attempts/{id}/finish finishes attempt and transitions to FINALIZING")
    void finishEndpoint() throws Exception {
        when(manageExamConnectionUseCase.finish(eq(attemptId), eq(studentId)))
                .thenReturn(buildView("FINALIZING"));

        mockMvc.perform(post("/api/v1/student/attempts/{id}/finish", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZING"));

        verify(manageExamConnectionUseCase).finish(eq(attemptId), eq(studentId));
    }
}
