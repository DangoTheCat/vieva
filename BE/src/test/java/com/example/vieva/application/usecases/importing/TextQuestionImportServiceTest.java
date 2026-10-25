package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.importing.TextQuestionImportCommand;
import com.example.vieva.application.ports.input.importing.TextQuestionImportResult;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextQuestionImportServiceTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private RubricTemplateRepository rubricTemplateRepository;
    @Mock private QuestionDraftWriter draftWriter;
    @Mock private SubjectAccessGuard accessGuard;
    @Mock private QuestionBankAuditor auditor;

    private TextQuestionImportServiceImpl service;

    private final UUID subjectId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TextQuestionImportServiceImpl(
                subjectRepository,
                rubricTemplateRepository,
                draftWriter,
                accessGuard,
                auditor
        );
    }

    @Test
    @DisplayName("importTextQuestions: valid 3 questions imported successfully")
    void importTextQuestions_Success() {
        Subject subject = Subject.builder().subjectId(subjectId).status(SubjectStatus.ACTIVE).build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        String input = "Question 1 content\nQuestion 2 content\nQuestion 3 content";
        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text(input)
                .bloomLevel("APPLY")
                .build();

        TextQuestionImportResult res = service.importTextQuestions(cmd, actorId);
        assertThat(res.getTotalImported()).isEqualTo(3);
        assertThat(res.getQuestionIds()).hasSize(3);
        verify(draftWriter).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("importTextQuestions: blank text content throws exception")
    void importTextQuestions_EmptyLine_ThrowsViolation() {
        Subject subject = Subject.builder().subjectId(subjectId).status(SubjectStatus.ACTIVE).build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        String input = "   \n   \n   ";
        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text(input)
                .bloomLevel("REMEMBER")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class);
        verify(draftWriter, never()).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("importTextQuestions: inactive subject throws SUBJECT_INACTIVE")
    void importTextQuestions_InactiveSubject_ThrowsSubjectInactive() {
        Subject subject = Subject.builder().subjectId(subjectId).status(SubjectStatus.INACTIVE).build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text("Question 1")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBJECT_INACTIVE);
    }
}
