package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RubricServiceImplTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionVersionRepository versionRepository;
    @Mock private RubricRepository rubricRepository;
    @Mock private RubricCriterionRepository criterionRepository;
    @Mock private QuestionSourceRepository sourceRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LecturerSubjectRepository lecturerSubjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort json;

    private RubricServiceImpl service;
    private final UUID lecturer = UUID.randomUUID();
    private QuestionVersion version;
    private Rubric rubric;
    private final List<RubricCriterion> criteria = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SubjectAccessGuard guard = new SubjectAccessGuard(lecturerSubjectRepository, userRepository);
        service = new RubricServiceImpl(versionRepository, rubricRepository, criterionRepository,
                new QuestionAccessLoader(questionRepository, versionRepository, guard),
                new QuestionVersionViewAssembler(questionRepository, topicRepository, rubricRepository, criterionRepository,
                        sourceRepository),
                new QuestionBankAuditor(auditEventRepository, json));

        Question question = Question.newDraftOwner(UUID.randomUUID(), null, lecturer);
        version = QuestionVersion.newDraft(question.getQuestionId(), 1, "c", "a", BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, lecturer);
        rubric = Rubric.create(version.getQuestionVersionId(), null, null);
        criteria.add(RubricCriterion.create(rubric.getRubricId(), "A", "d", new BigDecimal("4"), List.of(), 1));
        criteria.add(RubricCriterion.create(rubric.getRubricId(), "B", "d", new BigDecimal("6"), List.of(), 2));
        rubric.recalculateTotal(criteria);

        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, question.getSubjectId())).thenReturn(true);
        when(versionRepository.findById(version.getQuestionVersionId())).thenReturn(Optional.of(version));
        when(rubricRepository.findByQuestionVersionId(version.getQuestionVersionId())).thenReturn(Optional.of(rubric));
        when(criterionRepository.findByRubricId(rubric.getRubricId())).thenAnswer(inv -> new ArrayList<>(criteria));
    }

    @Test
    @DisplayName("adding/updating/deleting a criterion recomputes total = Σ max scores")
    void criterionCrudRecomputesTotal() {
        service.addCriterion(version.getQuestionVersionId(),
                new CriterionInput("C", "mô tả", new BigDecimal("2.5"), List.of(), null), null, lecturer);
        assertThat(rubric.getTotalPoints()).isEqualByComparingTo("12.5");

        service.updateCriterion(version.getQuestionVersionId(), criteria.get(0).getCriterionId(),
                new CriterionInput("A", "mô tả mới", new BigDecimal("1"), List.of(), null), null, lecturer);
        assertThat(rubric.getTotalPoints()).isEqualByComparingTo("7");

        service.deleteCriterion(version.getQuestionVersionId(), criteria.get(1).getCriterionId(), null, lecturer);
        assertThat(rubric.getTotalPoints()).isEqualByComparingTo("1");
        verify(versionRepository, org.mockito.Mockito.times(3)).save(version);

        assertThatThrownBy(() -> service.deleteCriterion(version.getQuestionVersionId(), UUID.randomUUID(), null, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("rubric of an APPROVED version is immutable")
    void approvedRubricIsImmutable() {
        version.approve(lecturer);
        assertThatThrownBy(() -> service.addCriterion(version.getQuestionVersionId(),
                new CriterionInput("C", "d", BigDecimal.ONE, List.of(), null), null, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.VERSION_NOT_DRAFT);
        verify(criterionRepository, never()).save(any());
    }
}
