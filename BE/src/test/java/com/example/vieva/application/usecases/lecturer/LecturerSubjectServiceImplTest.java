package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.AssignLecturerRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LecturerSubjectServiceImplTest {

    @Mock
    private LecturerSubjectRepository lecturerSubjectRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private LecturerSubjectServiceImpl lecturerSubjectService;

    private UUID adminId;
    private UUID lecturerId;
    private UUID subjectId;
    private Subject sampleSubject;
    private User sampleLecturer;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        lecturerId = UUID.randomUUID();
        subjectId = UUID.randomUUID();

        sampleSubject = Subject.builder()
                .subjectId(subjectId)
                .subjectCode("SWD392")
                .subjectName("Architecture")
                .status(SubjectStatus.ACTIVE)
                .build();

        Role lecturerRole = Role.builder().roleId(2).roleCode("ROLE_LECTURER").roleName("Lecturer").build();
        UserRole userRole = UserRole.builder().userId(lecturerId).roleId(2).role(lecturerRole).build();

        sampleLecturer = User.builder()
                .userId(lecturerId)
                .email("lecturer@fpt.edu.vn")
                .userCode("GV001")
                .status(UserStatus.ACTIVE)
                .userRoles(new HashSet<>(Set.of(userRole)))
                .build();
    }

    @Test
    @DisplayName("assignLecturersToSubject should succeed and record audit event")
    void assignLecturers_success() {
        AssignLecturerRequest request = AssignLecturerRequest.builder()
                .subjectId(subjectId)
                .lecturerIds(List.of(lecturerId))
                .build();

        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(lecturerId)).thenReturn(Optional.of(sampleLecturer));
        when(lecturerSubjectRepository.findActiveAssignment(lecturerId, subjectId)).thenReturn(Optional.empty());
        when(lecturerSubjectRepository.save(any(LecturerSubject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<LecturerSubject> result = lecturerSubjectService.assignLecturersToSubject(request, adminId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLecturerId()).isEqualTo(lecturerId);
        assertThat(result.get(0).getSubjectId()).isEqualTo(subjectId);
        assertThat(result.get(0).getIsActive()).isTrue();
        verify(lecturerSubjectRepository).save(any(LecturerSubject.class));
        verify(auditEventRepository).save(any());
    }

    @Test
    @DisplayName("assignLecturersToSubject should throw when user does not have LECTURER role")
    void assignLecturers_notALecturer_throwsException() {
        User regularUser = User.builder()
                .userId(lecturerId)
                .status(UserStatus.ACTIVE)
                .userRoles(new HashSet<>())
                .build();

        AssignLecturerRequest request = AssignLecturerRequest.builder()
                .subjectId(subjectId)
                .lecturerIds(List.of(lecturerId))
                .build();

        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(lecturerId)).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> lecturerSubjectService.assignLecturersToSubject(request, adminId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_A_LECTURER);

        verify(lecturerSubjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignLecturersToSubject should throw when already actively assigned")
    void assignLecturers_duplicate_throwsException() {
        AssignLecturerRequest request = AssignLecturerRequest.builder()
                .subjectId(subjectId)
                .lecturerIds(List.of(lecturerId))
                .build();

        LecturerSubject existing = LecturerSubject.builder()
                .lecturerSubjectId(UUID.randomUUID())
                .lecturerId(lecturerId)
                .subjectId(subjectId)
                .isActive(true)
                .build();

        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(lecturerId)).thenReturn(Optional.of(sampleLecturer));
        when(lecturerSubjectRepository.findActiveAssignment(lecturerId, subjectId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> lecturerSubjectService.assignLecturersToSubject(request, adminId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_ASSIGN_DUPLICATE);
    }

    @Test
    @DisplayName("revokeAssignment should set isActive=false and record audit event")
    void revokeAssignment_success() {
        UUID assignmentId = UUID.randomUUID();
        LecturerSubject existing = LecturerSubject.builder()
                .lecturerSubjectId(assignmentId)
                .lecturerId(lecturerId)
                .subjectId(subjectId)
                .isActive(true)
                .build();

        when(lecturerSubjectRepository.findById(assignmentId)).thenReturn(Optional.of(existing));
        when(lecturerSubjectRepository.save(any(LecturerSubject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lecturerSubjectService.revokeAssignment(assignmentId, adminId);

        assertThat(existing.getIsActive()).isFalse();
        assertThat(existing.getRevokedAt()).isNotNull();
        verify(lecturerSubjectRepository).save(existing);
        verify(auditEventRepository).save(any());
    }
}
