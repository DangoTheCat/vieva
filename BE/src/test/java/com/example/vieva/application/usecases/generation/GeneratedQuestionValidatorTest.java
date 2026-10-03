package com.example.vieva.application.usecases.generation;

import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate.Citation;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate.Criterion;
import com.example.vieva.application.usecases.generation.GeneratedQuestionValidator.Outcome;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.services.QuestionSimilarity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedQuestionValidatorTest {

    private ChunkSearchResult chunk;
    private Map<String, ChunkSearchResult> context;
    private final Set<BloomLevel> requested = EnumSet.of(BloomLevel.APPLY, BloomLevel.ANALYZE);

    @BeforeEach
    void setUp() {
        chunk = ChunkSearchResult.of(UUID.randomUUID(), UUID.randomUUID(), "csdl.pdf", 1, 3,
                "Chuẩn hoá 3NF loại bỏ phụ thuộc bắc cầu giữa các thuộc tính không khoá. Ví dụ bảng Nhân viên.", 0.2);
        context = Map.of("C1", chunk, chunk.chunkId().toString().toLowerCase(), chunk);
    }

    private GeneratedQuestionCandidate candidate(String bloom, BigDecimal total, List<Criterion> criteria,
                                                 List<String> refs, List<Citation> citations) {
        return new GeneratedQuestionCandidate("Hãy áp dụng chuẩn hoá 3NF cho bảng Nhân viên?",
                "Tách bảng để loại phụ thuộc bắc cầu.", bloom, total, criteria, refs, citations);
    }

    private static List<Criterion> criteria() {
        return List.of(new Criterion("Đúng quy trình", "0-6", new BigDecimal("6")),
                new Criterion("Giải thích", "0-4", new BigDecimal("4")));
    }

    private Outcome validate(GeneratedQuestionCandidate candidate) {
        return GeneratedQuestionValidator.validate(candidate, context, requested, List.of(), 0.8);
    }

    @Test
    @DisplayName("valid candidate keeps grounded citation and maps the source chunk")
    void validCandidate() {
        Outcome outcome = validate(candidate("APPLY", BigDecimal.TEN, criteria(), List.of("C1"),
                List.of(new Citation("C1", "loại bỏ phụ thuộc bắc cầu"))));
        assertThat(outcome.valid()).isTrue();
        assertThat(outcome.question().bloomLevel()).isEqualTo(BloomLevel.APPLY);
        assertThat(outcome.question().sources()).singleElement()
                .satisfies(s -> {
                    assertThat(s.chunk().chunkId()).isEqualTo(chunk.chunkId());
                    assertThat(s.quote()).isEqualTo("loại bỏ phụ thuộc bắc cầu");
                });
    }

    @Test
    @DisplayName("source given by UUID without quote falls back to a chunk excerpt")
    void uuidRefAndExcerpt() {
        Outcome outcome = validate(candidate("Phân tích", BigDecimal.TEN, criteria(),
                List.of(chunk.chunkId().toString().toUpperCase()), null));
        assertThat(outcome.valid()).isTrue();
        assertThat(chunk.content()).startsWith(outcome.question().sources().get(0).quote());
    }

    @Test
    void rejectsBloomOutsideSixLevelsOrNotRequested() {
        assertThat(validate(candidate("VAN_DUNG_CAO", BigDecimal.TEN, criteria(), List.of("C1"), null)).reasons())
                .anyMatch(r -> r.contains("not one of the 6 Bloom levels"));
        assertThat(validate(candidate("CREATE", BigDecimal.TEN, criteria(), List.of("C1"), null)).reasons())
                .anyMatch(r -> r.contains("was not requested"));
    }

    @Test
    void rejectsMissingOrForeignSources() {
        assertThat(validate(candidate("APPLY", BigDecimal.TEN, criteria(), List.of(), null)).reasons())
                .contains("no source chunk cited");
        assertThat(validate(candidate("APPLY", BigDecimal.TEN, criteria(), List.of("C9"), null)).reasons())
                .anyMatch(r -> r.contains("not one of the retrieved chunks"));
        assertThat(validate(candidate("APPLY", BigDecimal.TEN, criteria(), List.of("C1"),
                List.of(new Citation("C1", "câu bịa không có trong tài liệu")))).reasons())
                .anyMatch(r -> r.contains("not found verbatim"));
    }

    @Test
    void rejectsRubricTotalMismatchAndBadCriteria() {
        assertThat(validate(candidate("APPLY", new BigDecimal("12"), criteria(), List.of("C1"), null)).reasons())
                .anyMatch(r -> r.contains("differs from totalScore"));
        assertThat(validate(candidate("APPLY", BigDecimal.TEN, List.of(), List.of("C1"), null)).reasons())
                .contains("rubric has no criteria");
        assertThat(validate(candidate("APPLY", BigDecimal.TEN,
                List.of(new Criterion("A", "d", BigDecimal.ZERO)), List.of("C1"), null)).reasons())
                .anyMatch(r -> r.contains("no positive maxScore"));
    }

    @Test
    void rejectsMissingTextAndDuplicates() {
        GeneratedQuestionCandidate empty = new GeneratedQuestionCandidate(" ", null, "APPLY", BigDecimal.TEN,
                criteria(), List.of("C1"), null);
        assertThat(validate(empty).reasons()).contains("content is missing", "expectedAnswer is missing");

        GeneratedQuestionCandidate valid = candidate("APPLY", BigDecimal.TEN, criteria(), List.of("C1"), null);
        Outcome duplicate = GeneratedQuestionValidator.validate(valid, context, requested,
                List.of(QuestionSimilarity.tokens(valid.content())), 0.8);
        assertThat(duplicate.reasons()).contains("near-duplicate of an existing question");
    }
}
