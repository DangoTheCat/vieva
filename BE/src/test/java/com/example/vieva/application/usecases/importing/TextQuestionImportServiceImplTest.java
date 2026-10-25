package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.importing.TextQuestionImportCommand;
import com.example.vieva.application.ports.input.importing.TextQuestionImportResult;
import com.example.vieva.application.ports.input.question.CriterionInput;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.RubricTemplate;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextQuestionImportServiceImplTest {

    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;
    private RubricTemplateRepository rubricTemplateRepository;
    private QuestionDraftWriter draftWriter;
    private JsonSerializerPort jsonSerializer;
    private SubjectAccessGuard accessGuard;
    private QuestionBankAuditor auditor;
    private TextQuestionImportServiceImpl service;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        topicRepository = mock(TopicRepository.class);
        rubricTemplateRepository = mock(RubricTemplateRepository.class);
        draftWriter = mock(QuestionDraftWriter.class);
        jsonSerializer = mock(JsonSerializerPort.class);
        accessGuard = mock(SubjectAccessGuard.class);
        auditor = mock(QuestionBankAuditor.class);

        service = new TextQuestionImportServiceImpl(
                subjectRepository,
                topicRepository,
                rubricTemplateRepository,
                draftWriter,
                jsonSerializer,
                accessGuard,
                auditor
        );
    }

    @Test
    @DisplayName("Blank lines are safely ignored and reference answer is empty")
    void ignoresBlankLinesAndSetsEmptyAnswer() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        String rawText = "\n\n  Question 1: What is polymorphism?  \n\n\n Question 2: Explain encapsulation. \n\n  ";
        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text(rawText)
                .bloomLevel("UNDERSTAND")
                .build();

        TextQuestionImportResult result = service.importTextQuestions(cmd, actorId);

        assertThat(result.getTotalImported()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionDraftWriter.NewDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(draftWriter).saveQuestionDraft(captor.capture());

        List<QuestionDraftWriter.NewDraft> savedDrafts = captor.getValue();
        assertThat(savedDrafts).hasSize(2);
        assertThat(savedDrafts.get(0).version().getQuestionContent()).isEqualTo("Question 1: What is polymorphism?");
        assertThat(savedDrafts.get(0).version().getReferenceAnswer()).isEqualTo("");
        assertThat(savedDrafts.get(0).version().getBloomLevel()).isEqualTo(BloomLevel.UNDERSTAND);

        assertThat(savedDrafts.get(1).version().getQuestionContent()).isEqualTo("Question 2: Explain encapsulation.");
        assertThat(savedDrafts.get(1).version().getReferenceAnswer()).isEqualTo("");
    }

    @Test
    @DisplayName("All blank lines throw IMPORT_FILE_INVALID")
    void allBlankLinesThrowError() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text("\n\n   \n   \n\n")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMPORT_FILE_INVALID);

        verify(draftWriter, never()).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("Rejects invalid Bloom level with INVALID_REQUEST")
    void rejectsInvalidBloomLevel() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text("Valid question text")
                .bloomLevel("INVALID_BLOOM_LEVEL")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(draftWriter, never()).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("Rejects topic not in subject")
    void rejectsTopicNotInSubject() {
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(topicRepository.existsByIdAndSubjectId(topicId, subjectId)).thenReturn(false);

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .text("Valid question text")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOPIC_NOT_IN_SUBJECT);

        verify(draftWriter, never()).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("Rejects rubric template from different subject")
    void rejectsTemplateFromDifferentSubject() {
        UUID subjectId = UUID.randomUUID();
        UUID otherSubjectId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(otherSubjectId) // Different subject!
                .name("Other Template")
                .build();
        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .rubricTemplateId(templateId)
                .text("Valid question text")
                .build();

        assertThatThrownBy(() -> service.importTextQuestions(cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);

        verify(draftWriter, never()).saveQuestionDraft(any());
    }

    @Test
    @DisplayName("Applies actual criteria from rubric template")
    void appliesActualCriteriaFromTemplate() {
        UUID subjectId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .status(SubjectStatus.ACTIVE)
                .build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        String criteriaJson = "[{\"name\":\"Logic\",\"description\":\"Valid deduction\",\"maxScore\":10.0}]";
        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subjectId)
                .name("Logic Rubric")
                .description("Test template")
                .criteriaJson(criteriaJson)
                .build();
        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        CriterionInput[] parsed = new CriterionInput[]{
                new CriterionInput("Logic", "Valid deduction", BigDecimal.valueOf(10.0), List.of(), 1)
        };
        when(jsonSerializer.deserialize(eq(criteriaJson), eq(CriterionInput[].class))).thenReturn(parsed);

        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .rubricTemplateId(templateId)
                .text("What is deduction?")
                .build();

        TextQuestionImportResult result = service.importTextQuestions(cmd, actorId);
        assertThat(result.getTotalImported()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionDraftWriter.NewDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(draftWriter).saveQuestionDraft(captor.capture());

        List<QuestionDraftWriter.NewDraft> savedDrafts = captor.getValue();
        assertThat(savedDrafts.get(0).criteria()).hasSize(1);
        assertThat(savedDrafts.get(0).criteria().get(0).getCriterionName()).isEqualTo("Logic");
    }
}
