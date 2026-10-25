package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.exception.FieldViolation;
import com.example.vieva.application.ports.input.importing.TextQuestionImportCommand;
import com.example.vieva.application.ports.input.importing.TextQuestionImportResult;
import com.example.vieva.application.ports.input.importing.TextQuestionImportService;
import com.example.vieva.application.ports.input.question.CriterionInput;
import com.example.vieva.application.ports.input.question.RubricInput;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.QuestionDraftWriter.NewDraft;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.RubricDraftFactory;
import com.example.vieva.application.usecases.question.RubricDraftFactory.RubricDraft;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.RubricTemplate;
import com.example.vieva.domain.entities.SubjectStatus;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@UseCaseTransaction
public class TextQuestionImportServiceImpl implements TextQuestionImportService {

    private static final int MAX_QUESTIONS_PER_IMPORT = 2000;

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final RubricTemplateRepository rubricTemplateRepository;
    private final QuestionDraftWriter draftWriter;
    private final JsonSerializerPort jsonSerializer;
    private final SubjectAccessGuard accessGuard;
    private final QuestionBankAuditor auditor;

    public TextQuestionImportServiceImpl(
            SubjectRepository subjectRepository,
            RubricTemplateRepository rubricTemplateRepository,
            QuestionDraftWriter draftWriter,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        this(subjectRepository, null, rubricTemplateRepository, draftWriter, null, accessGuard, auditor);
    }

    @Override
    public TextQuestionImportResult importTextQuestions(TextQuestionImportCommand command, UUID actorId) {
        if (command == null || command.getSubjectId() == null || command.getText() == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Subject ID and text content are required");
        }

        accessGuard.requireManage(actorId, command.getSubjectId());
        subjectRepository.findById(command.getSubjectId())
                .filter(s -> s.getStatus() == SubjectStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_INACTIVE));

        // Validate topic if provided
        if (command.getTopicId() != null) {
            boolean topicValid = topicRepository.existsByIdAndSubjectId(command.getTopicId(), command.getSubjectId());
            if (!topicValid) {
                throw new ApplicationException(ErrorCode.TOPIC_NOT_IN_SUBJECT, "Topic does not belong to the specified subject");
            }
        }

        // Validate Bloom level if provided
        BloomLevel bloom = BloomLevel.REMEMBER;
        if (command.getBloomLevel() != null && !command.getBloomLevel().isBlank()) {
            bloom = BloomLevel.fromCodeOrLabel(command.getBloomLevel().trim())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Invalid Bloom level: " + command.getBloomLevel()));
        }

        String rawText = command.getText();
        String[] lines = rawText.split("\\r?\\n");

        if (lines.length > MAX_QUESTIONS_PER_IMPORT) {
            throw new ApplicationException(ErrorCode.IMPORT_LIMIT_EXCEEDED,
                    "Import exceeds maximum allowed " + MAX_QUESTIONS_PER_IMPORT + " questions (provided " + lines.length + ")");
        }

        List<FieldViolation> lineViolations = new ArrayList<>();
        List<String> validQuestionTexts = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i].trim();
            if (line.isEmpty()) {
                // Safely ignore blank lines
                continue;
            }
            if (line.length() > 5000) {
                lineViolations.add(FieldViolation.of(
                        "line_" + lineNum,
                        ErrorCode.INVALID_REQUEST,
                        "Dòng " + lineNum + ": Nội dung câu hỏi vượt quá 5000 ký tự"
                ));
            } else {
                validQuestionTexts.add(line);
            }
        }

        if (!lineViolations.isEmpty()) {
            throw ApplicationException.ofViolations(lineViolations);
        }

        if (validQuestionTexts.isEmpty()) {
            throw new ApplicationException(ErrorCode.IMPORT_FILE_INVALID, "No question content provided");
        }

        // Resolve Rubric Template if provided
        String templateName = "Rubric đánh giá nhập văn bản";
        String templateDesc = "Rubric mặc định cho câu hỏi nhập text";
        List<CriterionInput> criteriaInputs = null;

        if (command.getRubricTemplateId() != null) {
            RubricTemplate template = rubricTemplateRepository.findById(command.getRubricTemplateId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND));

            if (!template.getSubjectId().equals(command.getSubjectId())) {
                throw new ApplicationException(ErrorCode.FORBIDDEN_SUBJECT, "Rubric template subject does not match import subject");
            }

            templateName = template.getName();
            if (template.getDescription() != null && !template.getDescription().isBlank()) {
                templateDesc = template.getDescription();
            }

            if (template.getCriteriaJson() != null && !template.getCriteriaJson().isBlank()) {
                try {
                    CriterionInput[] parsed = jsonSerializer.deserialize(template.getCriteriaJson(), CriterionInput[].class);
                    if (parsed != null && parsed.length > 0) {
                        criteriaInputs = Arrays.asList(parsed);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (criteriaInputs == null || criteriaInputs.isEmpty()) {
            criteriaInputs = List.of(
                    new CriterionInput("Tiêu chuẩn đánh giá", "Mức độ chính xác của câu trả lời",
                            BigDecimal.valueOf(10.0), List.of(), 1)
            );
        }

        BigDecimal totalScore = criteriaInputs.stream()
                .map(CriterionInput::maxScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<NewDraft> drafts = new ArrayList<>(validQuestionTexts.size());
        List<UUID> createdIds = new ArrayList<>(validQuestionTexts.size());

        for (String qText : validQuestionTexts) {
            Question question = Question.newDraftOwner(command.getSubjectId(), command.getTopicId(), actorId);
            // No fake reference answer (store "" for blank draft answer)
            QuestionVersion version = QuestionVersion.newDraft(
                    question.getQuestionId(),
                    1,
                    qText,
                    "",
                    bloom,
                    QuestionGenerationMode.IMPORT,
                    actorId
            );

            List<FieldViolation> rubricViolations = new ArrayList<>();
            RubricDraft rubricDraft = RubricDraftFactory.build(
                    version.getQuestionVersionId(),
                    new RubricInput(templateName, templateDesc, totalScore, criteriaInputs),
                    rubricViolations
            );

            if (rubricDraft == null || !rubricViolations.isEmpty()) {
                throw ApplicationException.ofViolations(rubricViolations);
            }

            drafts.add(new NewDraft(question, version, rubricDraft.rubric(), rubricDraft.criteria(), List.of()));
            createdIds.add(question.getQuestionId());
        }

        draftWriter.saveQuestionDraft(drafts);

        auditor.record(actorId, "QUESTIONS_IMPORTED_TEXT", "SUBJECT", command.getSubjectId(),
                Map.of("count", drafts.size()));

        return TextQuestionImportResult.builder()
                .totalImported(drafts.size())
                .questionIds(createdIds)
                .build();
    }
}
