package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.CreateManualQuestionApiRequest;
import com.example.vieva.adapters.controllers.request.GenerateQuestionsRagApiRequest;
import com.example.vieva.adapters.controllers.request.RejectVersionApiRequest;
import com.example.vieva.adapters.controllers.request.RubricCriterionApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateQuestionDraftApiRequest;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.QuestionDetailDto;
import com.example.vieva.adapters.presenters.QuestionPresenter;
import com.example.vieva.adapters.presenters.QuestionVersionDto;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.usecases.question.QuestionBankService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.infrastructure.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LecturerQuestionControllerTest {

    @Mock
    private QuestionBankService questionBankService;

    @Mock
    private QuestionPresenter questionPresenter;

    @InjectMocks
    private LecturerQuestionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User authenticatedLecturer;
    private UUID subjectId;
    private UUID questionId;
    private UUID versionId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        subjectId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        authenticatedLecturer = User.builder()
                .userId(UUID.randomUUID())
                .email("lecturer@fpt.edu.vn")
                .fullName("Lecturer Test")
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return authenticatedLecturer;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("generateQuestionsAi returns 201 Created with generated question views")
    void generateQuestionsAi_success() throws Exception {
        GenerateQuestionsRagApiRequest request = GenerateQuestionsRagApiRequest.builder()
                .topicId(UUID.randomUUID())
                .bloomLevel(BloomLevel.APPLY)
                .quantity(2)
                .build();

        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder().questionId(questionId).build();

        when(questionBankService.generateQuestionsViaRag(eq(subjectId), any(), eq(authenticatedLecturer.getUserId())))
                .thenReturn(List.of(view));
        when(questionPresenter.toDtoList(List.of(view))).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/lecturer/subjects/{subjectId}/questions/generate-ai", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].questionId").value(questionId.toString()));
    }

    @Test
    @DisplayName("createManualQuestion returns 201 Created with question details")
    void createManualQuestion_success() throws Exception {
        CreateManualQuestionApiRequest request = CreateManualQuestionApiRequest.builder()
                .topicId(UUID.randomUUID())
                .questionContent("What is ACID?")
                .referenceAnswer("Atomicity, Consistency, Isolation, Durability.")
                .bloomLevel(BloomLevel.REMEMBER)
                .rubricName("ACID Rubric")
                .totalPoints(new BigDecimal("10.0"))
                .criteria(List.of(
                        RubricCriterionApiRequest.builder()
                                .criterionName("Criteria 1")
                                .maxPoints(new BigDecimal("10.0"))
                                .achievementDescriptors("Mo ta tieu chi day du")
                                .build()
                ))
                .build();

        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder().questionId(questionId).build();

        when(questionBankService.createManualQuestion(eq(subjectId), any(), eq(authenticatedLecturer.getUserId())))
                .thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/lecturer/subjects/{subjectId}/questions/manual", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionId").value(questionId.toString()));
    }

    @Test
    @DisplayName("searchQuestions returns 200 with paged question results")
    void searchQuestions_success() throws Exception {
        PagedResult<QuestionDetailView> paged = PagedResult.<QuestionDetailView>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();

        PageResponse<QuestionDetailDto> pageResponse = PageResponse.<QuestionDetailDto>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(questionBankService.searchQuestions(any())).thenReturn(paged);
        when(questionPresenter.toPageResponse(paged)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/lecturer/subjects/{subjectId}/questions", subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("getQuestionDetails returns 200 with detailed question info")
    void getQuestionDetails_success() throws Exception {
        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder().questionId(questionId).build();

        when(questionBankService.getQuestionDetails(questionId)).thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/lecturer/questions/{questionId}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(questionId.toString()));
    }

    @Test
    @DisplayName("createDraft returns 201 Created with new draft version")
    void createDraft_success() throws Exception {
        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder()
                .questionId(questionId)
                .hasPendingDraft(true)
                .build();

        when(questionBankService.createDraftFromApproved(questionId, authenticatedLecturer.getUserId()))
                .thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/lecturer/questions/{questionId}/create-draft", questionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasPendingDraft").value(true));
    }

    @Test
    @DisplayName("updateDraftVersion returns 200 with updated question details")
    void updateDraftVersion_success() throws Exception {
        UpdateQuestionDraftApiRequest request = UpdateQuestionDraftApiRequest.builder()
                .questionContent("Updated question")
                .referenceAnswer("Updated answer")
                .bloomLevel(BloomLevel.APPLY)
                .rubricName("Updated Rubric")
                .totalPoints(new BigDecimal("10.0"))
                .criteria(List.of(
                        RubricCriterionApiRequest.builder()
                                .criterionName("Criteria 1")
                                .maxPoints(new BigDecimal("10.0"))
                                .achievementDescriptors("Dat tieu chuan danh gia")
                                .build()
                ))
                .build();

        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder().questionId(questionId).build();

        when(questionBankService.updateDraftVersion(eq(questionId), eq(versionId), any(), eq(authenticatedLecturer.getUserId())))
                .thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/lecturer/questions/{questionId}/versions/{versionId}", questionId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(questionId.toString()));
    }

    @Test
    @DisplayName("approveVersion returns 200 with approved question details")
    void approveVersion_success() throws Exception {
        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder()
                .questionId(questionId)
                .activeVersion(QuestionVersionDto.builder().approvalStatus(QuestionApprovalStatus.APPROVED).build())
                .build();

        when(questionBankService.approveQuestionVersion(questionId, versionId, authenticatedLecturer.getUserId()))
                .thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/lecturer/questions/{questionId}/versions/{versionId}/approve", questionId, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeVersion.approvalStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("rejectVersion returns 200 with rejected draft version")
    void rejectVersion_success() throws Exception {
        RejectVersionApiRequest request = RejectVersionApiRequest.builder()
                .reason("Content does not meet syllabus")
                .build();

        QuestionDetailView view = QuestionDetailView.builder().build();
        QuestionDetailDto dto = QuestionDetailDto.builder().questionId(questionId).build();

        when(questionBankService.rejectQuestionVersion(questionId, versionId, "Content does not meet syllabus", authenticatedLecturer.getUserId()))
                .thenReturn(view);
        when(questionPresenter.toDto(view)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/lecturer/questions/{questionId}/versions/{versionId}/reject", questionId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(questionId.toString()));
    }

    @Test
    @DisplayName("deleteDraftVersion returns 200 message")
    void deleteDraftVersion_success() throws Exception {
        doNothing().when(questionBankService).deleteDraftVersion(questionId, versionId, authenticatedLecturer.getUserId());

        mockMvc.perform(delete("/api/v1/lecturer/questions/{questionId}/versions/{versionId}", questionId, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Draft version deleted successfully"));
    }

    @Test
    @DisplayName("archiveQuestion returns 200 message")
    void archiveQuestion_success() throws Exception {
        doNothing().when(questionBankService).archiveQuestion(questionId, authenticatedLecturer.getUserId());

        mockMvc.perform(patch("/api/v1/lecturer/questions/{questionId}/archive", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Question archived successfully"));
    }

    @Test
    @DisplayName("restoreQuestion returns 200 message")
    void restoreQuestion_success() throws Exception {
        doNothing().when(questionBankService).restoreQuestion(questionId, authenticatedLecturer.getUserId());

        mockMvc.perform(patch("/api/v1/lecturer/questions/{questionId}/restore", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Question restored successfully"));
    }
}
