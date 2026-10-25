package com.example.vieva.application.usecases.question;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.question.*;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RubricTemplateServiceTest {

    @Mock private RubricTemplateRepository rubricTemplateRepository;
    @Mock private RubricRepository rubricRepository;
    @Mock private RubricCriterionRepository rubricCriterionRepository;
    @Mock private QuestionVersionRepository questionVersionRepository;
    @Mock private com.example.vieva.application.ports.output.QuestionRepository questionRepository;
    @Mock private com.example.vieva.application.ports.output.JsonSerializerPort jsonSerializer;
    @Mock private SubjectAccessGuard accessGuard;
    @Mock private QuestionBankAuditor auditor;

    private RubricTemplateServiceImpl service;

    private final UUID subjectId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
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
    @DisplayName("createTemplate: valid command creates rubric template")
    void createTemplate_Success() {
        CreateRubricTemplateCommand cmd = CreateRubricTemplateCommand.builder()
                .subjectId(subjectId)
                .name("Presentation Rubric")
                .description("Scoring presentation")
                .criteriaJson("[{\"name\":\"Clarity\",\"maxScore\":10}]")
                .build();

        when(rubricTemplateRepository.save(any(RubricTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        RubricTemplateResult res = service.createTemplate(cmd, actorId);
        assertThat(res.getName()).isEqualTo("Presentation Rubric");
        assertThat(res.getSubjectId()).isEqualTo(subjectId);
        verify(rubricTemplateRepository).save(any(RubricTemplate.class));
    }

    @Test
    @DisplayName("copyTemplateToQuestionVersion: rejects non-draft version")
    void copyTemplate_NonDraft_ThrowsVersionNotDraft() {
        UUID versionId = UUID.randomUUID();
        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subjectId)
                .name("Standard")
                .criteriaJson("[]")
                .build();

        QuestionVersion nonDraft = QuestionVersion.builder()
                .questionVersionId(versionId)
                .approvalStatus(QuestionStatus.APPROVED)
                .build();

        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(nonDraft));

        assertThatThrownBy(() -> service.copyTemplateToQuestionVersion(templateId, versionId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.VERSION_NOT_DRAFT);
    }

    @Test
    @DisplayName("copyTemplateToQuestionVersion: draft version successfully copies template")
    void copyTemplate_Draft_Success() {
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        String criteriaJson = "[{\"name\":\"Logic\",\"description\":\"Sound reasoning\",\"maxScore\":10.0}]";
        RubricTemplate template = RubricTemplate.builder()
                .id(templateId)
                .subjectId(subjectId)
                .name("Standard")
                .criteriaJson(criteriaJson)
                .build();

        QuestionVersion draft = QuestionVersion.builder()
                .questionVersionId(versionId)
                .questionId(questionId)
                .approvalStatus(QuestionStatus.DRAFT)
                .build();

        com.example.vieva.domain.entities.Question question = com.example.vieva.domain.entities.Question.builder()
                .questionId(questionId)
                .subjectId(subjectId)
                .build();

        CriterionInput[] parsedInputs = new CriterionInput[]{
                new CriterionInput("Logic", "Sound reasoning", java.math.BigDecimal.TEN, List.of(), 1)
        };

        when(rubricTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(questionVersionRepository.findById(versionId)).thenReturn(Optional.of(draft));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(jsonSerializer.deserialize(org.mockito.ArgumentMatchers.eq(criteriaJson), org.mockito.ArgumentMatchers.eq(CriterionInput[].class)))
                .thenReturn(parsedInputs);
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.vieva.domain.entities.RubricCriterion c1 = com.example.vieva.domain.entities.RubricCriterion.create(
                UUID.randomUUID(), "Logic", "Sound reasoning", java.math.BigDecimal.TEN, List.of(), 1);
        when(rubricCriterionRepository.findByRubricId(any())).thenReturn(List.of(c1));

        RubricResult rubricRes = service.copyTemplateToQuestionVersion(templateId, versionId, actorId);
        assertThat(rubricRes).isNotNull();
        assertThat(rubricRes.name()).isEqualTo("Standard");
        verify(rubricRepository).save(any(Rubric.class));
        verify(rubricCriterionRepository).saveAll(any());
    }
}
