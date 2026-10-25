package com.example.vieva.application.usecases.question;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.question.CreateRubricTemplateCommand;
import com.example.vieva.application.ports.input.question.CriterionInput;
import com.example.vieva.application.ports.input.question.PerformanceLevelInput;
import com.example.vieva.application.ports.input.question.RubricResult;
import com.example.vieva.application.ports.input.question.RubricTemplateResult;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.PerformanceLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.RubricTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
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

class RubricTemplateServiceImplTest {

    private RubricTemplateRepository rubricTemplateRepository;
    private RubricRepository rubricRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private QuestionVersionRepository questionVersionRepository;
    private QuestionRepository questionRepository;
    private JsonSerializerPort jsonSerializer;
    private SubjectAccessGuard accessGuard;
    private QuestionBankAuditor auditor;
    private RubricTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        rubricTemplateRepository = mock(RubricTemplateRepository.class);
        rubricRepository = mock(RubricRepository.class);
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        questionVersionRepository = mock(QuestionVersionRepository.class);
        questionRepository = mock(QuestionRepository.class);
        jsonSerializer = mock(JsonSerializerPort.class);
        accessGuard = mock(SubjectAccessGuard.class);
        auditor = mock(QuestionBankAuditor.class);

        service = new RubricTemplateServiceImpl(
                rubricTemplateRepository,
                rubricRepository,
                rubricCriterionRepository,
                questionVersionRepository,
                questionRepository,
                jsonSerializer,
                accessGuard,
                auditor
        );
    }

    @Test
    @DisplayName("copyTemplateToQuestionVersion: copies actual template criteria to draft question")
    void copyTemplateCopiesActualCriteria() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        String criteriaJson = "[{\"name\":\"Logic\",\"description\":\"Sound reasoning\",\"maxScore\":6.0},{\"name\":\"Clarity\",\"description\":\"Clear writing\",\"maxScore\":4.0}]";

        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subjectId)
                .name("Analytical Rubric")
                .description("Two-part rubric")
                .criteriaJson(criteriaJson)
                .build();

        QuestionVersion draftVersion = QuestionVersion.builder()
                .questionVersionId(versionId)
                .questionId(questionId)
                .approvalStatus(QuestionStatus.DRAFT)
                .build();

        Question question = Question.builder()
                .questionId(questionId)
                .subjectId(subjectId)
                .build();

        CriterionInput[] parsedInputs = new CriterionInput[]{
                new CriterionInput("Logic", "Sound reasoning", BigDecimal.valueOf(6.0),
                        List.of(new PerformanceLevelInput("Excellent", "Perfect logic", BigDecimal.valueOf(6.0))), 1),
                new CriterionInput("Clarity", "Clear writing", BigDecimal.valueOf(4.0),
                        List.of(new PerformanceLevelInput("Clear", "Very clear", BigDecimal.valueOf(4.0))), 2)
        };

        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(jsonSerializer.deserialize(eq(criteriaJson), eq(CriterionInput[].class))).thenReturn(parsedInputs);
        when(rubricRepository.findByQuestionVersionId(versionId)).thenReturn(Optional.empty());

        RubricCriterion c1 = RubricCriterion.create(
                UUID.randomUUID(), "Logic", "Sound reasoning", BigDecimal.valueOf(6.0),
                List.of(new PerformanceLevel("Excellent", "Perfect logic", BigDecimal.valueOf(6.0))), 1
        );
        RubricCriterion c2 = RubricCriterion.create(
                UUID.randomUUID(), "Clarity", "Clear writing", BigDecimal.valueOf(4.0),
                List.of(new PerformanceLevel("Clear", "Very clear", BigDecimal.valueOf(4.0))), 2
        );

        when(rubricCriterionRepository.findByRubricId(any())).thenReturn(List.of(c1, c2));

        RubricResult result = service.copyTemplateToQuestionVersion(templateId, versionId, actorId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Analytical Rubric");
        assertThat(result.criteria()).hasSize(2);
        assertThat(result.criteria().get(0).name()).isEqualTo("Logic");
        assertThat(result.criteria().get(1).name()).isEqualTo("Clarity");

        verify(accessGuard).requireManage(actorId, subjectId);
        verify(rubricRepository).save(any(Rubric.class));
        verify(rubricCriterionRepository).saveAll(any());
    }

    @Test
    @DisplayName("copyTemplateToQuestionVersion: rejects non-DRAFT version")
    void copyTemplateRejectsNonDraft() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subjectId)
                .name("Template")
                .criteriaJson("[]")
                .build();

        QuestionVersion approvedVersion = QuestionVersion.builder()
                .questionVersionId(versionId)
                .questionId(questionId)
                .approvalStatus(QuestionStatus.APPROVED)
                .build();

        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(approvedVersion));

        assertThatThrownBy(() -> service.copyTemplateToQuestionVersion(templateId, versionId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VERSION_NOT_DRAFT);

        verify(rubricRepository, never()).save(any());
    }

    @Test
    @DisplayName("copyTemplateToQuestionVersion: rejects subject mismatch")
    void copyTemplateRejectsSubjectMismatch() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID subject1 = UUID.randomUUID();
        UUID subject2 = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subject1)
                .name("Template Subject 1")
                .criteriaJson("[]")
                .build();

        QuestionVersion draftVersion = QuestionVersion.builder()
                .questionVersionId(versionId)
                .questionId(questionId)
                .approvalStatus(QuestionStatus.DRAFT)
                .build();

        Question question = Question.builder()
                .questionId(questionId)
                .subjectId(subject2) // Mismatched subject
                .build();

        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.copyTemplateToQuestionVersion(templateId, versionId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_SUBJECT);

        verify(rubricRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTemplate: saves valid template and audits")
    void createTemplateSuccess() {
        UUID subjectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        CreateRubricTemplateCommand cmd = new CreateRubricTemplateCommand(subjectId, "General Rubric", "Desc", "[]");

        RubricTemplate saved = RubricTemplate.builder()
                .id(UUID.randomUUID())
                .subjectId(subjectId)
                .name("General Rubric")
                .description("Desc")
                .criteriaJson("[]")
                .build();

        when(rubricTemplateRepository.save(any(RubricTemplate.class))).thenReturn(saved);

        RubricTemplateResult result = service.createTemplate(cmd, actorId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("General Rubric");
        verify(accessGuard).requireManage(actorId, subjectId);
        verify(auditor).record(eq(actorId), eq("RUBRIC_TEMPLATE_CREATED"), eq("RUBRIC_TEMPLATE"), eq(saved.getId()), any());
    }
}
