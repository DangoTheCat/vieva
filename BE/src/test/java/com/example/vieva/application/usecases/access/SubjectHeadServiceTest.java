package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class SubjectHeadServiceTest {
    @Test
    void assignmentRoleAndActiveStatusAreRequiredAndAdminCannotReviewBypass() {
        var subjects=mock(SubjectRepository.class);
        var assignments=mock(LecturerSubjectRepository.class);
        var users=mock(UserRepository.class);
        var service=new SubjectHeadService(subjects,assignments,users);
        UUID subjectId=UUID.randomUUID(),head=UUID.randomUUID(),admin=UUID.randomUUID();
        Subject subject=Subject.builder().subjectId(subjectId).status(SubjectStatus.ACTIVE).headLecturerId(head).build();
        User lecturer=mock(User.class),administrator=mock(User.class);
        when(lecturer.isLecturer()).thenReturn(true); when(lecturer.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(administrator.isAdmin()).thenReturn(true); when(administrator.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(subjects.findByIdForUpdate(subjectId)).thenReturn(Optional.of(subject));
        when(users.findById(head)).thenReturn(Optional.of(lecturer));
        when(users.findById(admin)).thenReturn(Optional.of(administrator));
        when(assignments.isLecturerAssignedToSubject(head,subjectId)).thenReturn(true);
        assertThatCode(() -> service.requireHead(head,subjectId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireHead(admin,subjectId)).isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_SUBJECT_HEAD);
        service.assign(subjectId,head,admin); verify(subjects).save(subject);
        when(assignments.isLecturerAssignedToSubject(head,subjectId)).thenReturn(false);
        assertThatThrownBy(() -> service.requireHead(head,subjectId)).isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SUBJECT_HEAD_REQUIRED);
        when(assignments.isLecturerAssignedToSubject(head,subjectId)).thenReturn(true);
        when(lecturer.getStatus()).thenReturn(UserStatus.INACTIVE);
        assertThatThrownBy(() -> service.headOf(subjectId)).isInstanceOf(AppException.class);
    }
}
