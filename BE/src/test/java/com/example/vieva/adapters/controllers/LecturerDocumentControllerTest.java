package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.CourseDocumentDto;
import com.example.vieva.adapters.presenters.CourseDocumentPresenter;
import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentIndexingStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LecturerDocumentControllerTest {

    @Mock
    private DocumentIndexingService documentIndexingService;

    @Mock
    private CourseDocumentPresenter presenter;

    @InjectMocks
    private LecturerDocumentController controller;

    private MockMvc mockMvc;
    private User authenticatedLecturer;
    private UUID subjectId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
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
    @DisplayName("uploadDocument returns 202 Accepted on valid multipart file upload")
    void uploadDocument_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        CourseDocument doc = CourseDocument.builder()
                .documentId(documentId)
                .subjectId(subjectId)
                .fileName("syllabus.pdf")
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .build();

        CourseDocumentDto dto = CourseDocumentDto.builder()
                .documentId(documentId)
                .fileName("syllabus.pdf")
                .indexingStatus(DocumentIndexingStatus.UPLOADED)
                .build();

        when(documentIndexingService.uploadDocument(eq(subjectId), eq("syllabus.pdf"), any(), eq("application/pdf"), eq(authenticatedLecturer.getUserId())))
                .thenReturn(doc);
        when(presenter.toDto(doc)).thenReturn(dto);

        mockMvc.perform(multipart("/api/v1/lecturer/subjects/{subjectId}/documents", subjectId)
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.fileName").value("syllabus.pdf"));

        verify(documentIndexingService).uploadDocument(eq(subjectId), eq("syllabus.pdf"), any(), eq("application/pdf"), eq(authenticatedLecturer.getUserId()));
    }

    @Test
    @DisplayName("getDocumentsBySubject returns 200 with list of documents")
    void getDocumentsBySubject_success() throws Exception {
        CourseDocument doc = CourseDocument.builder().documentId(documentId).fileName("lecture.pdf").build();
        CourseDocumentDto dto = CourseDocumentDto.builder().documentId(documentId).fileName("lecture.pdf").build();

        when(documentIndexingService.getDocumentsBySubject(subjectId)).thenReturn(List.of(doc));
        when(presenter.toDtoList(List.of(doc))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/lecturer/subjects/{subjectId}/documents", subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(documentId.toString()))
                .andExpect(jsonPath("$[0].fileName").value("lecture.pdf"));
    }

    @Test
    @DisplayName("getDocumentById returns 200 with document details")
    void getDocumentById_success() throws Exception {
        CourseDocument doc = CourseDocument.builder().documentId(documentId).fileName("lecture.pdf").build();
        CourseDocumentDto dto = CourseDocumentDto.builder().documentId(documentId).fileName("lecture.pdf").build();

        when(documentIndexingService.getDocumentById(documentId)).thenReturn(doc);
        when(presenter.toDto(doc)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/lecturer/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()));
    }

    @Test
    @DisplayName("deleteDocument returns 200 message")
    void deleteDocument_success() throws Exception {
        doNothing().when(documentIndexingService).softDeleteDocument(documentId, authenticatedLecturer.getUserId());

        mockMvc.perform(delete("/api/v1/lecturer/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));

        verify(documentIndexingService).softDeleteDocument(documentId, authenticatedLecturer.getUserId());
    }
}
