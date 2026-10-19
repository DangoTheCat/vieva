package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSecurityEvaluatorTest {

    @Mock
    private LecturerSubjectRepository lecturerSubjectRepository;

    @Mock
    private CourseDocumentRepository courseDocumentRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionVersionRepository questionVersionRepository;

    @Mock
    private QuestionGenerationRequestRepository generationRequestRepository;

    @InjectMocks
    private CourseSecurityEvaluator evaluator;

    private UUID subjectId;
    private UUID lecturerId;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        lecturerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Lecturer assigned to subject -> canAccessSubject returns true")
    void canAccessSubject_lecturerAssigned_returnsTrue() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturerId, subjectId)).thenReturn(true);

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Lecturer not assigned to subject -> canAccessSubject returns false")
    void canAccessSubject_lecturerNotAssigned_returnsFalse() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturerId, subjectId)).thenReturn(false);

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Admin user -> canAccessSubject returns true even if not assigned")
    void canAccessSubject_admin_returnsTrue() {
        UUID adminId = UUID.randomUUID();
        ActorView actor = new ActorView(adminId, "admin@example.com", Set.of("ROLE_ADMIN"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Unauthenticated or null auth -> canAccessSubject returns false")
    void canAccessSubject_unauthenticated_returnsFalse() {
        boolean nullAuthAllowed = evaluator.canAccessSubject(subjectId, null);
        assertThat(nullAuthAllowed).isFalse();

        Authentication unauth = new UsernamePasswordAuthenticationToken("anon", null, Collections.emptyList());
        unauth.setAuthenticated(false);
        boolean unauthAllowed = evaluator.canAccessSubject(subjectId, unauth);
        assertThat(unauthAllowed).isFalse();
    }
}
