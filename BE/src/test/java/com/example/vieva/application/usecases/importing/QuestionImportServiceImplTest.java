package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.ImportReport;
import com.example.vieva.application.ports.output.ImportReport.RowError;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.settings.ImportSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.QuestionDraftWriter;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.infrastructure.service.PoiCsvQuestionSpreadsheetGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC1.6 with the real CSV/XLSX adapter and mocked persistence.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionImportServiceImplTest {

    private static final String HEADER =
            "question_ref,topic,content,expected_answer,bloom_level,criterion_name,criterion_description,criterion_max_score\n";

    @Mock private SubjectRepository subjectRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionVersionRepository versionRepository;
    @Mock private RubricRepository rubricRepository;
    @Mock private RubricCriterionRepository criterionRepository;
    @Mock private QuestionSourceRepository sourceRepository;
    @Mock private LecturerSubjectRepository lecturerSubjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort json;

    private QuestionImportServiceImpl service;
    private final ImportSettings settings = new ImportSettings();
    private final UUID lecturer = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestionImportServiceImpl(new PoiCsvQuestionSpreadsheetGateway(), subjectRepository, topicRepository,
                new SubjectAccessGuard(lecturerSubjectRepository, userRepository),
                new QuestionDraftWriter(questionRepository, versionRepository, rubricRepository, criterionRepository,
                        sourceRepository),
                new QuestionBankAuditor(auditEventRepository, json), settings);
        when(lecturerSubjectRepository.isLecturerAssignedToSubject(lecturer, subjectId)).thenReturn(true);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(Subject.builder().subjectId(subjectId).build()));
        when(topicRepository.findBySubjectId(subjectId)).thenReturn(List.of(
                Topic.builder().topicId(UUID.randomUUID()).subjectId(subjectId).topicName("Giao dịch").orderIndex(1).build()));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static byte[] csv(String body) {
        return (HEADER + body).getBytes(StandardCharsets.UTF_8);
    }

    private static final String MIXED = String.join("\n",
            "Q1,giao dịch,Nêu ACID?,Atomicity...,Hiểu,Đủ ý,0-4,4",
            "Q1,,,,,Ví dụ,0-6,6",
            "Q2,Chỉ mục,Phân tích B-tree?,Cây cân bằng,PHAN_TICH_SAU,Đúng,0-10,10",
            "Q3,,Thiết kế lược đồ?,Lược đồ 3NF,CREATE,Đúng,,abc",
            ",,,,,,,",
            "Q4,Chỉ mục,So sánh hash và B-tree?,Hash O(1),Đánh giá,So sánh,0-5,\"2,5\"") + "\n";

    @Test
    @DisplayName("dry-run reports row/column errors and writes nothing")
    void dryRun() {
        ImportReport report = service.importQuestions(subjectId, "questions.csv", csv(MIXED), true, lecturer);

        assertThat(report.dryRun()).isTrue();
        assertThat(report.totalRows()).isEqualTo(5);
        assertThat(report.totalQuestions()).isEqualTo(4);
        assertThat(report.validQuestions()).isEqualTo(2);
        assertThat(report.invalidQuestions()).isEqualTo(2);
        assertThat(report.createdQuestions()).isZero();
        assertThat(report.errors()).extracting(RowError::row, RowError::column, RowError::questionRef)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(4, "bloom_level", "Q2"),
                        org.assertj.core.groups.Tuple.tuple(5, "criterion_description", "Q3"),
                        org.assertj.core.groups.Tuple.tuple(5, "criterion_max_score", "Q3"));
        verify(versionRepository, never()).saveAll(anyList());
        verify(topicRepository, never()).save(any());
    }

    @Test
    @DisplayName("commit saves valid questions as IMPORT drafts, reuses/creates topics and skips invalid ones")
    void commit() {
        ImportReport report = service.importQuestions(subjectId, "questions.csv", csv(MIXED), false, lecturer);

        assertThat(report.createdQuestions()).isEqualTo(2);
        assertThat(report.createdQuestionIds()).hasSize(2);
        ArgumentCaptor<List<QuestionVersion>> versions = ArgumentCaptor.forClass(List.class);
        verify(versionRepository).saveAll(versions.capture());
        assertThat(versions.getValue()).extracting(QuestionVersion::getBloomLevel)
                .containsExactly(BloomLevel.UNDERSTAND, BloomLevel.EVALUATE);
        assertThat(versions.getValue()).allSatisfy(v -> {
            assertThat(v.getGenerationMode()).isEqualTo(QuestionGenerationMode.IMPORT);
            assertThat(v.isDraft()).isTrue();
            assertThat(v.isBloomConfirmed()).isTrue();
        });
        ArgumentCaptor<List<Rubric>> rubrics = ArgumentCaptor.forClass(List.class);
        verify(rubricRepository).saveAll(rubrics.capture());
        assertThat(rubrics.getValue()).extracting(r -> r.getTotalPoints().stripTrailingZeros().toPlainString())
                .containsExactly("10", "2.5");
        // "giao dịch" matches the existing topic case-insensitively; "Chỉ mục" is created once.
        verify(topicRepository, times(1)).save(any());
        verify(auditEventRepository).save(any());
    }

    @Test
    void rejectsBadFiles() {
        assertThatThrownBy(() -> service.importQuestions(subjectId, "q.txt", csv(MIXED), true, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        assertThatThrownBy(() -> service.importQuestions(subjectId, "q.csv", "a,b\n1,2\n".getBytes(), true, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.IMPORT_FILE_INVALID);
        assertThatThrownBy(() -> service.importQuestions(subjectId, "q.csv", HEADER.getBytes(), true, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.IMPORT_FILE_INVALID);
        settings.setMaxRows(2);
        assertThatThrownBy(() -> service.importQuestions(subjectId, "q.csv", csv(MIXED), true, lecturer))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.IMPORT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("templates (xlsx/csv) can be imported back as-is")
    void templateRoundTrip() {
        for (String format : List.of("xlsx", "csv")) {
            byte[] template = service.template(format);
            ImportReport report = service.importQuestions(subjectId, "template." + format, template, true, lecturer);
            assertThat(report.errors()).as(format).isEmpty();
            assertThat(report.validQuestions()).as(format).isEqualTo(2);
        }
        assertThatThrownBy(() -> service.template("pdf"))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
