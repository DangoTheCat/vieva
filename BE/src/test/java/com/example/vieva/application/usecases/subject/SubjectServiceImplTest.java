package com.example.vieva.application.usecases.subject;

import com.example.vieva.application.ports.input.CreateSubjectRequest;
import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.application.ports.input.UpdateSubjectRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private UUID adminId;
    private UUID subjectId;
    private Subject sampleSubject;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        sampleSubject = Subject.builder()
                .subjectId(subjectId)
                .subjectCode("SWD392")
                .subjectName("Software Architecture & Design")
                .description("Course on software architecture")
                .credits(3)
                .status(SubjectStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createSubject should succeed and record audit event")
    void createSubject_success() {
        CreateSubjectRequest request = CreateSubjectRequest.builder()
                .subjectCode("swd392")
                .subjectName("Software Architecture & Design")
                .credits(3)
                .status(SubjectStatus.ACTIVE)
                .build();

        when(subjectRepository.existsBySubjectCode("SWD392")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject result = subjectService.createSubject(request, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getSubjectCode()).isEqualTo("SWD392");
        assertThat(result.getSubjectName()).isEqualTo("Software Architecture & Design");
        verify(subjectRepository).save(any(Subject.class));
        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("createSubject should throw when subject code already exists")
    void createSubject_duplicateCode_throwsException() {
        CreateSubjectRequest request = CreateSubjectRequest.builder()
                .subjectCode("SWD392")
                .subjectName("Software Architecture & Design")
                .build();

        when(subjectRepository.existsBySubjectCode("SWD392")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.createSubject(request, adminId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBJECT_CODE_EXISTED);

        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSubject should update properties and record audit event")
    void updateSubject_success() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectName("Updated Course Name")
                .credits(4)
                .build();

        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(sampleSubject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject updated = subjectService.updateSubject(subjectId, request, adminId);

        assertThat(updated.getSubjectName()).isEqualTo("Updated Course Name");
        assertThat(updated.getCredits()).isEqualTo(4);
        verify(subjectRepository).save(sampleSubject);
        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("deactivateSubject should set status to INACTIVE and record audit event")
    void deactivateSubject_success() {
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(sampleSubject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subjectService.deactivateSubject(subjectId, adminId);

        assertThat(sampleSubject.getStatus()).isEqualTo(SubjectStatus.INACTIVE);
        verify(subjectRepository).save(sampleSubject);
        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("getSubjectById should throw when subject not found")
    void getSubjectById_notFound_throwsException() {
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getSubjectById(subjectId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBJECT_NOT_FOUND);
    }
}
