package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.ports.output.ImportReport;
import com.example.vieva.application.ports.output.ImportReport.RowError;
import com.example.vieva.application.ports.output.ImportRow;
import com.example.vieva.application.ports.output.QuestionSpreadsheetPort;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.settings.ImportSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.QuestionDraftWriter;
import com.example.vieva.application.usecases.question.QuestionDraftWriter.NewDraft;
import com.example.vieva.application.usecases.question.RubricDraftFactory;
import com.example.vieva.application.usecases.question.RubricDraftFactory.RubricDraft;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    public static final String COL_REF = "question_ref";
    public static final String COL_TOPIC = "topic";
    public static final String COL_CONTENT = "content";
    public static final String COL_ANSWER = "expected_answer";
    public static final String COL_BLOOM = "bloom_level";
    public static final String COL_CRITERION_NAME = "criterion_name";
    public static final String COL_CRITERION_DESCRIPTION = "criterion_description";
    public static final String COL_CRITERION_MAX = "criterion_max_score";

    public static final List<String> HEADERS = List.of(COL_REF, COL_TOPIC, COL_CONTENT, COL_ANSWER, COL_BLOOM,
            COL_CRITERION_NAME, COL_CRITERION_DESCRIPTION, COL_CRITERION_MAX);
    private static final List<String> REQUIRED_HEADERS = HEADERS.stream().filter(h -> !h.equals(COL_TOPIC)).toList();

    private final QuestionSpreadsheetPort spreadsheet;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionDraftWriter draftWriter;
    private final QuestionBankAuditor auditor;
    private final ImportSettings settings;

    /** One question assembled from its rows, ready to persist when {@code errors} is empty. */
    private record ParsedQuestion(String ref, int firstRow, String topicName, String content, String answer,
                                  BloomLevel bloomLevel, List<CriterionInput> criteria, List<RowError> errors) {
    }

    @Override
    public byte[] template(String format) {
        String normalized = normalizeFormat(format);
        List<List<String>> samples = List.of(
                List.of("Q1", "Giao dịch", "Trình bày 4 tính chất ACID của giao dịch.",
                        "Atomicity, Consistency, Isolation, Durability và ý nghĩa của từng tính chất.",
                        "UNDERSTAND", "Nêu đủ tính chất", "0: không nêu; 2: nêu 2/4; 4: nêu đủ 4", "4"),
                List.of("Q1", "Giao dịch", "", "", "", "Giải thích ý nghĩa",
                        "0: sai; 3: giải thích một phần; 6: giải thích đúng, có ví dụ", "6"),
                List.of("Q2", "", "Hãy áp dụng chuẩn hoá 3NF cho lược đồ quan hệ cho trước.",
                        "Tách lược đồ theo phụ thuộc hàm, loại phụ thuộc bắc cầu.",
                        "Vận dụng", "Tách lược đồ đúng", "0: sai; 5: đúng một phần; 10: đúng hoàn toàn", "10"));
        return spreadsheet.template(normalized, HEADERS, samples);
    }

    @Override
    @Transactional
    public ImportReport importQuestions(UUID subjectId, String filename, byte[] content, boolean dryRun, UUID actorId) {
        subjectAccessGuard.requireManage(actorId, subjectId);
        if (subjectRepository.findById(subjectId).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        validateFile(filename, content);

        List<ImportRow> rows = spreadsheet.read(content, filename).stream()
                .filter(row -> !row.isBlank())
                .toList();
        if (rows.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "The file has no data rows");
        }
        if (rows.size() > settings.getMaxRows()) {
            throw new AppException(ErrorCode.IMPORT_LIMIT_EXCEEDED,
                    "The file has " + rows.size() + " rows; the limit is " + settings.getMaxRows());
        }
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> !rows.get(0).values().containsKey(header))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "Missing columns: " + String.join(", ", missingHeaders));
        }

        Map<String, Topic> topicsByName = topicRepository.findBySubjectId(subjectId).stream()
                .filter(topic -> topic.getTopicName() != null)
                .collect(Collectors.toMap(topic -> key(topic.getTopicName()), topic -> topic, (a, b) -> a));

        List<RowError> errors = new ArrayList<>();
        List<ParsedQuestion> questions = parse(rows, errors);
        List<ParsedQuestion> valid = questions.stream().filter(q -> q.errors().isEmpty()).toList();
        questions.forEach(q -> errors.addAll(q.errors()));
        errors.sort((a, b) -> Integer.compare(a.row(), b.row()));

        List<UUID> createdIds = new ArrayList<>();
        if (!dryRun && !valid.isEmpty()) {
            List<NewDraft> drafts = new ArrayList<>();
            for (ParsedQuestion parsed : valid) {
                Topic topic = resolveTopic(subjectId, parsed.topicName(), topicsByName);
                Question question = Question.newDraftOwner(subjectId, topic == null ? null : topic.getTopicId(), actorId);
                QuestionVersion version = QuestionVersion.newDraft(question.getQuestionId(), 1, parsed.content(),
                        parsed.answer(), parsed.bloomLevel(), QuestionGenerationMode.IMPORT, actorId);
                List<FieldViolation> violations = new ArrayList<>();
                RubricDraft rubric = RubricDraftFactory.build(version.getQuestionVersionId(),
                        new RubricInput(null, null, null, parsed.criteria()), violations);
                drafts.add(new NewDraft(question, version, rubric.rubric(), rubric.criteria(), List.of()));
                createdIds.add(question.getQuestionId());
            }
            draftWriter.persist(drafts);
            Map<String, Object> audit = new HashMap<>();
            audit.put("fileName", filename);
            audit.put("created", drafts.size());
            audit.put("invalid", questions.size() - valid.size());
            auditor.record(actorId, "QUESTIONS_IMPORTED", QuestionBankAuditor.SUBJECT, subjectId, audit);
        }

        return ImportReport.builder()
                .dryRun(dryRun)
                .totalRows(rows.size())
                .totalQuestions(questions.size())
                .validQuestions(valid.size())
                .invalidQuestions(questions.size() - valid.size())
                .createdQuestions(createdIds.size())
                .errors(errors)
                .createdQuestionIds(createdIds)
                .build();
    }

    private List<ParsedQuestion> parse(List<ImportRow> rows, List<RowError> errors) {
        Map<String, List<ImportRow>> groups = new LinkedHashMap<>();
        for (ImportRow row : rows) {
            String ref = row.get(COL_REF);
            if (ref == null || ref.isBlank()) {
                errors.add(new RowError(row.rowNumber(), COL_REF, null, "question_ref is required"));
                continue;
            }
            groups.computeIfAbsent(ref, k -> new ArrayList<>()).add(row);
        }
        List<ParsedQuestion> questions = new ArrayList<>();
        groups.forEach((ref, group) -> questions.add(parseGroup(ref, group)));
        return questions;
    }

    private ParsedQuestion parseGroup(String ref, List<ImportRow> group) {
        List<RowError> errors = new ArrayList<>();
        int firstRow = group.get(0).rowNumber();
        String content = consistentValue(ref, group, COL_CONTENT, errors);
        String answer = consistentValue(ref, group, COL_ANSWER, errors);
        String bloomRaw = consistentValue(ref, group, COL_BLOOM, errors);
        String topic = consistentValue(ref, group, COL_TOPIC, errors);

        if (content == null) {
            errors.add(new RowError(firstRow, COL_CONTENT, ref, "content is required"));
        }
        if (answer == null) {
            errors.add(new RowError(firstRow, COL_ANSWER, ref, "expected_answer is required"));
        }
        BloomLevel bloom = null;
        if (bloomRaw == null) {
            errors.add(new RowError(firstRow, COL_BLOOM, ref, "bloom_level is required"));
        } else {
            bloom = BloomLevel.fromCodeOrLabel(bloomRaw).orElse(null);
            if (bloom == null) {
                errors.add(new RowError(firstRow, COL_BLOOM, ref, "bloom_level '" + bloomRaw + "' must be one of "
                        + Arrays.stream(BloomLevel.values()).map(l -> l.name() + "/" + l.getViLabel())
                        .collect(Collectors.joining(", "))));
            }
        }
        if (topic != null && topic.length() > 255) {
            errors.add(new RowError(firstRow, COL_TOPIC, ref, "topic is longer than 255 characters"));
        }

        List<CriterionInput> criteria = new ArrayList<>();
        for (ImportRow row : group) {
            String name = row.get(COL_CRITERION_NAME);
            String description = row.get(COL_CRITERION_DESCRIPTION);
            String rawScore = row.get(COL_CRITERION_MAX);
            boolean rowValid = true;
            if (isBlank(name)) {
                errors.add(new RowError(row.rowNumber(), COL_CRITERION_NAME, ref, "criterion_name is required"));
                rowValid = false;
            }
            if (isBlank(description)) {
                errors.add(new RowError(row.rowNumber(), COL_CRITERION_DESCRIPTION, ref, "criterion_description is required"));
                rowValid = false;
            }
            BigDecimal score = parseScore(rawScore);
            if (score == null || score.signum() <= 0) {
                errors.add(new RowError(row.rowNumber(), COL_CRITERION_MAX, ref,
                        "criterion_max_score must be a number greater than 0"));
                rowValid = false;
            }
            if (rowValid) {
                criteria.add(new CriterionInput(name, description, score, List.of(), criteria.size() + 1));
            }
        }
        if (errors.isEmpty()) {
            // Same rules as the API (BR-02): criteria present, positive scores, total = Σ.
            List<FieldViolation> violations = new ArrayList<>();
            RubricDraftFactory.build(UUID.randomUUID(), new RubricInput(null, null, null, criteria), violations);
            violations.forEach(v -> errors.add(new RowError(firstRow, null, ref, v.message())));
        }
        return new ParsedQuestion(ref, firstRow, topic, content, answer, bloom, criteria, errors);
    }

    /** Question-level columns may be filled on the first row only, or repeated identically. */
    private String consistentValue(String ref, List<ImportRow> group, String column, List<RowError> errors) {
        String value = null;
        for (ImportRow row : group) {
            String cell = row.get(column);
            if (isBlank(cell)) {
                continue;
            }
            if (value == null) {
                value = cell;
            } else if (!value.equals(cell)) {
                errors.add(new RowError(row.rowNumber(), column, ref,
                        column + " differs from the first row of question " + ref));
            }
        }
        return value;
    }

    private Topic resolveTopic(UUID subjectId, String name, Map<String, Topic> topicsByName) {
        if (isBlank(name)) {
            return null;
        }
        return topicsByName.computeIfAbsent(key(name), k -> {
            int nextOrder = topicsByName.values().stream()
                    .map(Topic::getOrderIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            return topicRepository.save(Topic.builder()
                    .topicId(UUID.randomUUID())
                    .subjectId(subjectId)
                    .topicName(name.trim())
                    .orderIndex(nextOrder)
                    .createdAt(Instant.now())
                    .build());
        });
    }

    private void validateFile(String filename, byte[] content) {
        if (content == null || content.length == 0) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "The file is empty");
        }
        if (content.length > settings.getMaxFileSizeBytes()) {
            throw new AppException(ErrorCode.IMPORT_LIMIT_EXCEEDED,
                    "The file exceeds " + (settings.getMaxFileSizeBytes() / 1024) + " KB");
        }
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith("." + FORMAT_XLSX) && !lower.endsWith("." + FORMAT_CSV)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Import accepts .xlsx or .csv files");
        }
    }

    static BigDecimal parseScore(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeFormat(String format) {
        String normalized = format == null ? FORMAT_XLSX : format.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(FORMAT_XLSX) && !normalized.equals(FORMAT_CSV)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "format must be xlsx or csv");
        }
        return normalized;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
