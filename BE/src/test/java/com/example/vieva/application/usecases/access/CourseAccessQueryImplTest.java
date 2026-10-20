package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessQueryImplTest {

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
    private CourseAccessQueryImpl query;

    private UUID subjectId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        subjectId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("canAccessSubject")
    class SubjectAccessTests {

        @Test
        @DisplayName("null subjectId or actorId -> false")
        void nullInputs_returnsFalse() {
            assertThat(query.canAccessSubject(null, actorId, false)).isFalse();
            assertThat(query.canAccessSubject(subjectId, null, false)).isFalse();
            assertThat(query.canAccessSubject(null, null, false)).isFalse();
            assertThat(query.canAccessSubject(null, actorId, true)).isFalse();
            assertThat(query.canAccessSubject(subjectId, null, true)).isFalse();
        }

        @Test
        @DisplayName("isAdmin -> true without checking repository")
        void admin_returnsTrue() {
            assertThat(query.canAccessSubject(subjectId, actorId, true)).isTrue();
            verifyNoInteractions(lecturerSubjectRepository);
        }

        @Test
        @DisplayName("Lecturer assigned -> true")
        void assigned_returnsTrue() {
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(true);
            assertThat(query.canAccessSubject(subjectId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("Lecturer not assigned -> false")
        void notAssigned_returnsFalse() {
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(false);
            assertThat(query.canAccessSubject(subjectId, actorId, false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canAccessDocument")
    class DocumentAccessTests {

        private UUID documentId;

        @BeforeEach
        void initDoc() {
            documentId = UUID.randomUUID();
        }

        @Test
        @DisplayName("null documentId or actorId -> false")
        void nullInputs_returnsFalse() {
            assertThat(query.canAccessDocument(null, actorId, false)).isFalse();
            assertThat(query.canAccessDocument(documentId, null, false)).isFalse();
        }

        @Test
        @DisplayName("isAdmin -> true")
        void admin_returnsTrue() {
            assertThat(query.canAccessDocument(documentId, actorId, true)).isTrue();
            verifyNoInteractions(courseDocumentRepository);
        }

        @Test
        @DisplayName("document not found -> true (so use case can 404)")
        void notFound_returnsTrue() {
            when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.empty());
            assertThat(query.canAccessDocument(documentId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("document found and lecturer assigned -> true")
        void foundAndAssigned_returnsTrue() {
            CourseDocument doc = CourseDocument.builder().subjectId(subjectId).build();
            when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(true);

            assertThat(query.canAccessDocument(documentId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("document found and lecturer not assigned -> false")
        void foundAndNotAssigned_returnsFalse() {
            CourseDocument doc = CourseDocument.builder().subjectId(subjectId).build();
            when(courseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(false);

            assertThat(query.canAccessDocument(documentId, actorId, false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canAccessQuestion")
    class QuestionAccessTests {

        private UUID questionId;

        @BeforeEach
        void initQuestion() {
            questionId = UUID.randomUUID();
        }

        @Test
        @DisplayName("null questionId or actorId -> false")
        void nullInputs_returnsFalse() {
            assertThat(query.canAccessQuestion(null, actorId, false)).isFalse();
            assertThat(query.canAccessQuestion(questionId, null, false)).isFalse();
        }

        @Test
        @DisplayName("isAdmin -> true")
        void admin_returnsTrue() {
            assertThat(query.canAccessQuestion(questionId, actorId, true)).isTrue();
            verifyNoInteractions(questionRepository);
        }

        @Test
        @DisplayName("question not found -> true")
        void notFound_returnsTrue() {
            when(questionRepository.findById(questionId)).thenReturn(Optional.empty());
            assertThat(query.canAccessQuestion(questionId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("question found and lecturer assigned -> true")
        void foundAndAssigned_returnsTrue() {
            Question question = Question.builder().subjectId(subjectId).build();
            when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(true);

            assertThat(query.canAccessQuestion(questionId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("question found and lecturer not assigned -> false")
        void foundAndNotAssigned_returnsFalse() {
            Question question = Question.builder().subjectId(subjectId).build();
            when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(false);

            assertThat(query.canAccessQuestion(questionId, actorId, false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canAccessVersion")
    class VersionAccessTests {

        private UUID versionId;
        private UUID questionId;

        @BeforeEach
        void initVersion() {
            versionId = UUID.randomUUID();
            questionId = UUID.randomUUID();
        }

        @Test
        @DisplayName("null versionId or actorId -> false")
        void nullInputs_returnsFalse() {
            assertThat(query.canAccessVersion(null, actorId, false)).isFalse();
            assertThat(query.canAccessVersion(versionId, null, false)).isFalse();
        }

        @Test
        @DisplayName("isAdmin -> true")
        void admin_returnsTrue() {
            assertThat(query.canAccessVersion(versionId, actorId, true)).isTrue();
            verifyNoInteractions(questionVersionRepository);
        }

        @Test
        @DisplayName("version not found -> true")
        void notFound_returnsTrue() {
            when(questionVersionRepository.findById(versionId)).thenReturn(Optional.empty());
            assertThat(query.canAccessVersion(versionId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("version found, question not found -> true")
        void questionNotFound_returnsTrue() {
            QuestionVersion version = QuestionVersion.builder().questionId(questionId).build();
            when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
            when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

            assertThat(query.canAccessVersion(versionId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("version found, question found, assigned -> true")
        void foundAndAssigned_returnsTrue() {
            QuestionVersion version = QuestionVersion.builder().questionId(questionId).build();
            Question question = Question.builder().subjectId(subjectId).build();
            when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
            when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(true);

            assertThat(query.canAccessVersion(versionId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("version found, question found, not assigned -> false")
        void foundAndNotAssigned_returnsFalse() {
            QuestionVersion version = QuestionVersion.builder().questionId(questionId).build();
            Question question = Question.builder().subjectId(subjectId).build();
            when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
            when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(false);

            assertThat(query.canAccessVersion(versionId, actorId, false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canAccessGenerationRequest")
    class GenerationRequestAccessTests {

        private UUID requestId;

        @BeforeEach
        void initRequest() {
            requestId = UUID.randomUUID();
        }

        @Test
        @DisplayName("null requestId or actorId -> false")
        void nullInputs_returnsFalse() {
            assertThat(query.canAccessGenerationRequest(null, actorId, false)).isFalse();
            assertThat(query.canAccessGenerationRequest(requestId, null, false)).isFalse();
        }

        @Test
        @DisplayName("isAdmin -> true")
        void admin_returnsTrue() {
            assertThat(query.canAccessGenerationRequest(requestId, actorId, true)).isTrue();
            verifyNoInteractions(generationRequestRepository);
        }

        @Test
        @DisplayName("generation request not found -> true")
        void notFound_returnsTrue() {
            when(generationRequestRepository.findById(requestId)).thenReturn(Optional.empty());
            assertThat(query.canAccessGenerationRequest(requestId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("generation request found and assigned -> true")
        void foundAndAssigned_returnsTrue() {
            QuestionGenerationRequest req = QuestionGenerationRequest.builder().subjectId(subjectId).build();
            when(generationRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(true);

            assertThat(query.canAccessGenerationRequest(requestId, actorId, false)).isTrue();
        }

        @Test
        @DisplayName("generation request found and not assigned -> false")
        void foundAndNotAssigned_returnsFalse() {
            QuestionGenerationRequest req = QuestionGenerationRequest.builder().subjectId(subjectId).build();
            when(generationRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
            when(lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)).thenReturn(false);

            assertThat(query.canAccessGenerationRequest(requestId, actorId, false)).isFalse();
        }
    }
}
