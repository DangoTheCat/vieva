package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.usecases.document.DocumentFileValidator;
import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.domain.entities.PerformanceLevel;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RubricDraftFactoryTest {

    private static CriterionInput criterion(String name, String score) {
        return new CriterionInput(name, "mô tả mức đạt", score == null ? null : new BigDecimal(score), List.of(), null);
    }

    @Test
    @DisplayName("total is computed as Σ max scores when omitted")
    void computesTotal() {
        List<FieldViolation> violations = new ArrayList<>();
        RubricDraftFactory.RubricDraft draft = RubricDraftFactory.build(UUID.randomUUID(),
                new RubricInput("R", null, null, List.of(criterion("A", "3.5"), criterion("B", "6.5"))), violations);
        assertThat(violations).isEmpty();
        assertThat(draft.rubric().getTotalPoints()).isEqualByComparingTo("10");
        assertThat(draft.criteria()).extracting("orderIndex").containsExactly(1, 2);
        assertThat(draft.criteria()).allMatch(c -> c.getRubricId().equals(draft.rubric().getRubricId()));
    }

    @Test
    @DisplayName("given total must match Σ (RUBRIC_SCORE_MISMATCH)")
    void rejectsMismatchedTotal() {
        List<FieldViolation> violations = new ArrayList<>();
        assertThat(RubricDraftFactory.build(UUID.randomUUID(),
                new RubricInput(null, null, BigDecimal.TEN, List.of(criterion("A", "4"))), violations)).isNull();
        assertThat(violations).extracting(FieldViolation::field, FieldViolation::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("rubric.totalScore", ErrorCode.RUBRIC_SCORE_MISMATCH));
    }

    @Test
    @DisplayName("collects field errors per criterion")
    void collectsFieldErrors() {
        List<FieldViolation> violations = new ArrayList<>();
        CriterionInput badLevel = new CriterionInput("C", "d", new BigDecimal("2"),
                List.of(new PerformanceLevel("Giỏi", null, new BigDecimal("3"))), null);
        RubricDraftFactory.build(UUID.randomUUID(), new RubricInput(null, null, null,
                List.of(criterion(" ", "1"), criterion("B", "0"), badLevel)), violations);
        assertThat(violations).extracting(FieldViolation::field).containsExactly(
                "rubric.criteria[0].name", "rubric.criteria[1].maxScore", "rubric.criteria[2].levels[0].score");

        List<FieldViolation> empty = new ArrayList<>();
        RubricDraftFactory.build(UUID.randomUUID(), new RubricInput(null, null, null, List.of()), empty);
        assertThat(empty).extracting(FieldViolation::code).containsExactly(ErrorCode.RUBRIC_REQUIRED);
    }

    @Test
    @DisplayName("upload checks: file name sanitizing, size and real MIME vs extension")
    void documentFileValidation() {
        DocumentSettings settings = new DocumentSettings();
        settings.setMaxSizeBytes(10);
        assertThat(DocumentFileValidator.sanitizeFilename("..\\..\\etc/Giáo trình <CSDL>.pdf")).isEqualTo("Giáo trình _CSDL_.pdf");
        assertThat(DocumentFileValidator.sanitizeFilename("....")).isEqualTo("document");

        assertThatThrownBy(() -> DocumentFileValidator.validateSize(new byte[0], settings))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> DocumentFileValidator.validateSize(new byte[11], settings))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);

        assertThat(DocumentFileValidator.validateType("a.PDF", "application/pdf", settings)).isEqualTo("application/pdf");
        assertThat(DocumentFileValidator.validateType("a.txt", "text/plain; charset=UTF-8", settings)).isEqualTo("text/plain");
        assertThatThrownBy(() -> DocumentFileValidator.validateType("a.pdf", "text/plain", settings))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        assertThatThrownBy(() -> DocumentFileValidator.validateType("a.exe", "application/x-msdownload", settings))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        settings.setAllowedExtensions(List.of("pdf"));
        assertThatThrownBy(() -> DocumentFileValidator.validateType("a.txt", "text/plain", settings))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
