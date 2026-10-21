package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.input.access.CourseAccessQuery;
import com.example.vieva.application.ports.input.common.ActorView;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSecurityEvaluatorTest {

    @Mock
    private CourseAccessQuery courseAccessQuery;

    @InjectMocks
    private CourseSecurityEvaluator evaluator;

    private UUID subjectId;
    private UUID documentId;
    private UUID questionId;
    private UUID versionId;
    private UUID requestId;
    private UUID lecturerId;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        lecturerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Lecturer assigned to subject -> canAccessSubject delegates and returns true")
    void canAccessSubject_lecturerAssigned_returnsTrue() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessSubject(subjectId, lecturerId, false)).thenReturn(true);

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessSubject(subjectId, lecturerId, false);
    }

    @Test
    @DisplayName("Lecturer not assigned to subject -> canAccessSubject delegates and returns false")
    void canAccessSubject_lecturerNotAssigned_returnsFalse() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessSubject(subjectId, lecturerId, false)).thenReturn(false);

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isFalse();
        verify(courseAccessQuery).canAccessSubject(subjectId, lecturerId, false);
    }

    @Test
    @DisplayName("Admin user -> canAccessSubject passes isAdmin=true to query port")
    void canAccessSubject_admin_returnsTrue() {
        UUID adminId = UUID.randomUUID();
        ActorView actor = new ActorView(adminId, "admin@example.com", Set.of("ROLE_ADMIN"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(courseAccessQuery.canAccessSubject(subjectId, adminId, true)).thenReturn(true);

        boolean allowed = evaluator.canAccessSubject(subjectId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessSubject(subjectId, adminId, true);
    }

    @Test
    @DisplayName("Delegates canAccessDocument to courseAccessQuery")
    void canAccessDocument_delegates() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessDocument(documentId, lecturerId, false)).thenReturn(true);

        boolean allowed = evaluator.canAccessDocument(documentId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessDocument(documentId, lecturerId, false);
    }

    @Test
    @DisplayName("Delegates canAccessQuestion to courseAccessQuery")
    void canAccessQuestion_delegates() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessQuestion(questionId, lecturerId, false)).thenReturn(true);

        boolean allowed = evaluator.canAccessQuestion(questionId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessQuestion(questionId, lecturerId, false);
    }

    @Test
    @DisplayName("Delegates canAccessQuestionVersion to courseAccessQuery.canAccessVersion")
    void canAccessQuestionVersion_delegates() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessVersion(versionId, lecturerId, false)).thenReturn(true);

        boolean allowed = evaluator.canAccessQuestionVersion(versionId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessVersion(versionId, lecturerId, false);
    }

    @Test
    @DisplayName("Delegates canAccessGenerationRequest to courseAccessQuery")
    void canAccessGenerationRequest_delegates() {
        ActorView actor = new ActorView(lecturerId, "lecturer@example.com", Set.of("ROLE_LECTURER"), false);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));

        when(courseAccessQuery.canAccessGenerationRequest(requestId, lecturerId, false)).thenReturn(true);

        boolean allowed = evaluator.canAccessGenerationRequest(requestId, auth);

        assertThat(allowed).isTrue();
        verify(courseAccessQuery).canAccessGenerationRequest(requestId, lecturerId, false);
    }

    @Test
    @DisplayName("Unauthenticated or null auth -> returns false without calling query port")
    void unauthenticated_returnsFalse() {
        boolean nullAuthAllowed = evaluator.canAccessSubject(subjectId, null);
        assertThat(nullAuthAllowed).isFalse();

        Authentication unauth = new UsernamePasswordAuthenticationToken("anon", null, Collections.emptyList());
        unauth.setAuthenticated(false);
        boolean unauthAllowed = evaluator.canAccessSubject(subjectId, unauth);
        assertThat(unauthAllowed).isFalse();
    }
}
