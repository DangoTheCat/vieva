package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CreateManualQuestionRequest;
import com.example.vieva.application.ports.input.GenerateQuestionsRagRequest;
import com.example.vieva.application.ports.input.QuestionSearchCriteria;
import com.example.vieva.application.ports.input.RubricCriterionInput;
import com.example.vieva.application.ports.input.UpdateQuestionDraftRequest;
import com.example.vieva.application.ports.output.AiQuestionGeneratorPort;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.GeneratedCriterionItem;
import com.example.vieva.application.ports.output.GeneratedQuestionItem;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final TopicRepository topicRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingModelPort embeddingModel;
    private final AiQuestionGeneratorPort aiQuestionGenerator;

    @Value("${vieva.rag.similarity-threshold:0.60}")
    private double similarityThreshold;

    @Override
    @Transactional
    public List<QuestionDetailView> generateQuestionsViaRag(UUID subjectId, GenerateQuestionsRagRequest request, UUID lecturerId) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));

        if (!topic.getSubjectId().equals(subjectId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_IN_SUBJECT);
        }

        // 1. Prepare retrieval query
        String queryText = topic.getTopicName() + (request.getCustomPrompt() != null ? " " + request.getCustomPrompt() : "");
        float[] queryEmbedding = embeddingModel.generateEmbedding(queryText);

        // Max distance = 1.0 - threshold
        double maxDistance = Math.max(0.1, 1.0 - similarityThreshold);
        List<ChunkSearchResult> chunks = documentChunkRepository.searchSimilarChunks(subjectId, queryEmbedding, maxDistance, 6);

        if (chunks == null || chunks.isEmpty()) {
            throw new AppException(ErrorCode.INSUFFICIENT_CONTEXT,
                    "Không tìm thấy tài liệu phù hợp trong môn học cho chủ đề này (ngưỡng tương đồng >= " + similarityThreshold + ")");
        }

        // 2. Call AI Question Generator
        List<GeneratedQuestionItem> generatedItems = aiQuestionGenerator.generateQuestions(
                topic.getTopicName(),
                request.getBloomLevel(),
                request.getQuantity(),
                request.getCustomPrompt(),
                chunks
        );

        List<Question> questionsToSave = new ArrayList<>();
        List<QuestionVersion> versionsToSave = new ArrayList<>();
        List<QuestionSource> sourcesToSave = new ArrayList<>();
        List<Rubric> rubricsToSave = new ArrayList<>();
        List<RubricCriterion> criteriaToSave = new ArrayList<>();

        for (GeneratedQuestionItem item : generatedItems) {
            // 3. Create Question parent record
            UUID questionId = UUID.randomUUID();
            String questionCode = "Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Question question = Question.builder()
                    .questionId(questionId)
                    .topicId(topic.getTopicId())
                    .questionCode(questionCode)
                    .status(QuestionStatus.ACTIVE)
                    .createdBy(lecturerId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            questionsToSave.add(question);

            // 4. Create QuestionVersion (v1, DRAFT, AI_RAG)
            UUID versionId = UUID.randomUUID();
            QuestionVersion version = QuestionVersion.builder()
                    .questionVersionId(versionId)
                    .questionId(questionId)
                    .versionNumber(1)
                    .questionContent(item.questionContent())
                    .referenceAnswer(item.referenceAnswer())
                    .bloomLevel(item.bloomLevel())
                    .generationMode(QuestionGenerationMode.AI_RAG)
                    .approvalStatus(QuestionApprovalStatus.DRAFT)
                    .createdBy(lecturerId)
                    .createdAt(Instant.now())
                    .build();
            versionsToSave.add(version);

            // 5. Create QuestionSource (citation provenance)
            UUID sourceId = UUID.randomUUID();
            QuestionSource source = QuestionSource.builder()
                    .questionSourceId(sourceId)
                    .questionVersionId(versionId)
                    .chunkId(item.chunkId())
                    .documentName(item.documentName())
                    .citationQuote(item.citationQuote())
                    .similarityScore(BigDecimal.valueOf(item.similarityScore()))
                    .createdAt(Instant.now())
                    .build();
            sourcesToSave.add(source);

            // 6. Create Rubric & Criteria
            UUID rubricId = UUID.randomUUID();
            Rubric rubric = Rubric.builder()
                    .rubricId(rubricId)
                    .questionVersionId(versionId)
                    .rubricName(item.rubricName())
                    .totalPoints(item.totalPoints())
                    .description(item.rubricDescription())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            List<RubricCriterion> criteriaList = new ArrayList<>();
            if (item.criteria() != null) {
                for (GeneratedCriterionItem c : item.criteria()) {
                    RubricCriterion criterion = RubricCriterion.builder()
                            .criterionId(UUID.randomUUID())
                            .rubricId(rubricId)
                            .criterionName(c.criterionName())
                            .maxPoints(c.maxPoints())
                            .achievementDescriptors(c.achievementDescriptors())
                            .orderIndex(c.orderIndex())
                            .createdAt(Instant.now())
                            .build();
                    criteriaList.add(criterion);
                }
            }

            // Invariant check on Rubric Aggregate
            try {
                rubric.validateTotalPoints(criteriaList);
            } catch (Exception e) {
                log.warn("Auto-correcting rubric totalPoints for generated question", e);
                BigDecimal sum = criteriaList.stream()
                        .map(RubricCriterion::getMaxPoints)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                rubric.setTotalPoints(sum);
            }

            rubricsToSave.add(rubric);
            criteriaToSave.addAll(criteriaList);
        }

        // Batched persistence: 5 round-trips total instead of ~5 per generated item (Rule 4).
        Map<UUID, Question> savedQuestions = questionRepository.saveAll(questionsToSave).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q, (first, second) -> first));
        Map<UUID, QuestionVersion> savedVersions = questionVersionRepository.saveAll(versionsToSave).stream()
                .collect(Collectors.toMap(QuestionVersion::getQuestionVersionId, v -> v, (first, second) -> first));
        Map<UUID, QuestionSource> savedSources = questionSourceRepository.saveAll(sourcesToSave).stream()
                .collect(Collectors.toMap(QuestionSource::getQuestionSourceId, s -> s, (first, second) -> first));
        Map<UUID, Rubric> savedRubrics = rubricRepository.saveAll(rubricsToSave).stream()
                .collect(Collectors.toMap(Rubric::getRubricId, r -> r, (first, second) -> first));
        Map<UUID, List<RubricCriterion>> savedCriteriaByRubric = rubricCriterionRepository.saveAll(criteriaToSave).stream()
                .collect(Collectors.groupingBy(RubricCriterion::getRubricId));

        List<QuestionDetailView> results = new ArrayList<>();
        for (int i = 0; i < questionsToSave.size(); i++) {
            Question savedQuestion = savedQuestions.get(questionsToSave.get(i).getQuestionId());
            QuestionVersion savedVersion = savedVersions.get(versionsToSave.get(i).getQuestionVersionId());
            QuestionSource savedSource = savedSources.get(sourcesToSave.get(i).getQuestionSourceId());
            Rubric savedRubric = savedRubrics.get(rubricsToSave.get(i).getRubricId());
            List<RubricCriterion> savedCriteria = savedRubric != null
                    ? savedCriteriaByRubric.getOrDefault(savedRubric.getRubricId(), List.of())
                    : List.of();

            results.add(QuestionDetailView.builder()
                    .question(savedQuestion)
                    .activeVersion(null)
                    .draftVersion(savedVersion)
                    .hasPendingDraft(true)
                    .rubric(savedRubric)
                    .criteria(savedCriteria)
                    .sources(savedSource != null ? List.of(savedSource) : List.of())
                    .versionHistory(savedVersion != null ? List.of(savedVersion) : List.of())
                    .build());
        }

        return results;
    }

    @Override
    @Transactional
    public QuestionDetailView createManualQuestion(UUID subjectId, CreateManualQuestionRequest request, UUID lecturerId) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));

        if (!topic.getSubjectId().equals(subjectId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_IN_SUBJECT);
        }

        // Validate Rubric invariant
        List<RubricCriterion> criteria = new ArrayList<>();
        UUID rubricId = UUID.randomUUID();
        if (request.getCriteria() != null) {
            int order = 1;
            for (RubricCriterionInput input : request.getCriteria()) {
                criteria.add(RubricCriterion.builder()
                        .criterionId(UUID.randomUUID())
                        .rubricId(rubricId)
                        .criterionName(input.getCriterionName())
                        .maxPoints(input.getMaxPoints())
                        .achievementDescriptors(input.getAchievementDescriptors())
                        .orderIndex(input.getOrderIndex() != null ? input.getOrderIndex() : order++)
                        .createdAt(Instant.now())
                        .build());
            }
        }

        Rubric rubric = Rubric.builder()
                .rubricId(rubricId)
                .rubricName(request.getRubricName())
                .totalPoints(request.getTotalPoints())
                .description(request.getRubricDescription())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        try {
            rubric.validateTotalPoints(criteria);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_RUBRIC_TOTAL, e.getMessage());
        }

        // 1. Create Question
        UUID questionId = UUID.randomUUID();
        String questionCode = "Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Question question = Question.builder()
                .questionId(questionId)
                .topicId(topic.getTopicId())
                .questionCode(questionCode)
                .status(QuestionStatus.ACTIVE)
                .createdBy(lecturerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Question savedQuestion = questionRepository.save(question);

        // 2. Create QuestionVersion (v1, DRAFT, MANUAL)
        UUID versionId = UUID.randomUUID();
        QuestionVersion version = QuestionVersion.builder()
                .questionVersionId(versionId)
                .questionId(questionId)
                .versionNumber(1)
                .questionContent(request.getQuestionContent())
                .referenceAnswer(request.getReferenceAnswer())
                .bloomLevel(request.getBloomLevel())
                .generationMode(QuestionGenerationMode.MANUAL)
                .approvalStatus(QuestionApprovalStatus.DRAFT)
                .createdBy(lecturerId)
                .createdAt(Instant.now())
                .build();
        QuestionVersion savedVersion = questionVersionRepository.save(version);

        // 3. Link Rubric & Criteria
        rubric.setQuestionVersionId(versionId);
        Rubric savedRubric = rubricRepository.save(rubric);
        List<RubricCriterion> savedCriteria = rubricCriterionRepository.saveAll(criteria);

        return QuestionDetailView.builder()
                .question(savedQuestion)
                .activeVersion(null)
                .draftVersion(savedVersion)
                .hasPendingDraft(true)
                .rubric(savedRubric)
                .criteria(savedCriteria)
                .sources(Collections.emptyList())
                .versionHistory(List.of(savedVersion))
                .build();
    }

    @Override
    @Transactional
    public QuestionDetailView createDraftFromApproved(UUID questionId, UUID lecturerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        if (questionVersionRepository.hasDraftVersion(questionId)) {
            throw new AppException(ErrorCode.DRAFT_ALREADY_EXISTS,
                    "Câu hỏi này đã có một bản nháp DRAFT đang chờ duyệt. Vui lòng duyệt hoặc xóa bản nháp cũ trước.");
        }

        QuestionVersion activeApproved = questionVersionRepository.findActiveApprovedVersion(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND,
                        "Không tìm thấy phiên bản APPROVED để nhân bản"));

        // Copy-on-Write: create new version
        UUID newVersionId = UUID.randomUUID();
        QuestionVersion newDraft = QuestionVersion.builder()
                .questionVersionId(newVersionId)
                .questionId(questionId)
                .versionNumber(activeApproved.getVersionNumber() + 1)
                .questionContent(activeApproved.getQuestionContent())
                .referenceAnswer(activeApproved.getReferenceAnswer())
                .bloomLevel(activeApproved.getBloomLevel())
                .generationMode(activeApproved.getGenerationMode())
                .approvalStatus(QuestionApprovalStatus.DRAFT)
                .createdBy(lecturerId)
                .createdAt(Instant.now())
                .build();
        QuestionVersion savedDraft = questionVersionRepository.save(newDraft);

        // Copy Rubric & Criteria
        Optional<Rubric> oldRubricOpt = rubricRepository.findByQuestionVersionId(activeApproved.getQuestionVersionId());
        Rubric savedRubric = null;
        List<RubricCriterion> savedCriteria = new ArrayList<>();

        if (oldRubricOpt.isPresent()) {
            Rubric oldRubric = oldRubricOpt.get();
            UUID newRubricId = UUID.randomUUID();
            Rubric newRubric = Rubric.builder()
                    .rubricId(newRubricId)
                    .questionVersionId(newVersionId)
                    .rubricName(oldRubric.getRubricName())
                    .totalPoints(oldRubric.getTotalPoints())
                    .description(oldRubric.getDescription())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            savedRubric = rubricRepository.save(newRubric);

            List<RubricCriterion> oldCriteria = rubricCriterionRepository.findByRubricId(oldRubric.getRubricId());
            for (RubricCriterion c : oldCriteria) {
                savedCriteria.add(RubricCriterion.builder()
                        .criterionId(UUID.randomUUID())
                        .rubricId(newRubricId)
                        .criterionName(c.getCriterionName())
                        .maxPoints(c.getMaxPoints())
                        .achievementDescriptors(c.getAchievementDescriptors())
                        .orderIndex(c.getOrderIndex())
                        .createdAt(Instant.now())
                        .build());
            }
            savedCriteria = rubricCriterionRepository.saveAll(savedCriteria);
        }

        // Copy sources if AI_RAG
        List<QuestionSource> oldSources = questionSourceRepository.findByQuestionVersionId(activeApproved.getQuestionVersionId());
        List<QuestionSource> newSources = new ArrayList<>();
        for (QuestionSource s : oldSources) {
            newSources.add(QuestionSource.builder()
                    .questionSourceId(UUID.randomUUID())
                    .questionVersionId(newVersionId)
                    .chunkId(s.getChunkId())
                    .documentName(s.getDocumentName())
                    .citationQuote(s.getCitationQuote())
                    .similarityScore(s.getSimilarityScore())
                    .createdAt(Instant.now())
                    .build());
        }
        if (!newSources.isEmpty()) {
            newSources = questionSourceRepository.saveAll(newSources);
        }

        return getQuestionDetails(questionId);
    }

    @Override
    @Transactional
    public QuestionDetailView updateDraftVersion(UUID questionId, UUID versionId, UpdateQuestionDraftRequest request, UUID lecturerId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

        if (!version.getQuestionId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "QuestionVersion does not belong to Question");
        }

        if (version.getApprovalStatus() != QuestionApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_NON_DRAFT,
                    "Chỉ bản ghi DRAFT mới được phép chỉnh sửa trực tiếp. Vui lòng tạo bản nháp mới.");
        }

        // Update version content
        version.setQuestionContent(request.getQuestionContent());
        version.setReferenceAnswer(request.getReferenceAnswer());
        version.setBloomLevel(request.getBloomLevel());
        questionVersionRepository.save(version);

        // Update Rubric & Criteria
        Optional<Rubric> rubricOpt = rubricRepository.findByQuestionVersionId(versionId);
        Rubric rubric = rubricOpt.orElseGet(() -> Rubric.builder()
                .rubricId(UUID.randomUUID())
                .questionVersionId(versionId)
                .createdAt(Instant.now())
                .build());

        rubric.setRubricName(request.getRubricName());
        rubric.setTotalPoints(request.getTotalPoints());
        rubric.setDescription(request.getRubricDescription());
        rubric.setUpdatedAt(Instant.now());

        List<RubricCriterion> criteria = new ArrayList<>();
        if (request.getCriteria() != null) {
            int order = 1;
            for (RubricCriterionInput input : request.getCriteria()) {
                criteria.add(RubricCriterion.builder()
                        .criterionId(UUID.randomUUID())
                        .rubricId(rubric.getRubricId())
                        .criterionName(input.getCriterionName())
                        .maxPoints(input.getMaxPoints())
                        .achievementDescriptors(input.getAchievementDescriptors())
                        .orderIndex(input.getOrderIndex() != null ? input.getOrderIndex() : order++)
                        .createdAt(Instant.now())
                        .build());
            }
        }

        try {
            rubric.validateTotalPoints(criteria);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_RUBRIC_TOTAL, e.getMessage());
        }

        rubricRepository.save(rubric);
        rubricCriterionRepository.deleteByRubricId(rubric.getRubricId());
        rubricCriterionRepository.saveAll(criteria);

        return getQuestionDetails(questionId);
    }

    @Override
    @Transactional
    public QuestionDetailView approveQuestionVersion(UUID questionId, UUID versionId, UUID lecturerId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

        if (!version.getQuestionId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (version.getApprovalStatus() != QuestionApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_NON_DRAFT, "Chỉ bản ghi DRAFT mới có thể duyệt");
        }

        // 1. Verify Rubric exists and totalPoints matches criteria
        Rubric rubric = rubricRepository.findByQuestionVersionId(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RUBRIC_TOTAL, "Câu hỏi bắt buộc phải có Rubric"));
        List<RubricCriterion> criteria = rubricCriterionRepository.findByRubricId(rubric.getRubricId());
        if (criteria.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_RUBRIC_TOTAL, "Rubric bắt buộc phải có ít nhất 1 tiêu chí");
        }

        try {
            rubric.validateTotalPoints(criteria);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_RUBRIC_TOTAL, e.getMessage());
        }

        // 2. Verify Citation for AI_RAG questions
        List<QuestionSource> sources = questionSourceRepository.findByQuestionVersionId(versionId);
        if (version.getGenerationMode() == QuestionGenerationMode.AI_RAG) {
            try {
                QuestionSource.validateRequiredForAiApproved(version.getGenerationMode(), QuestionApprovalStatus.APPROVED, sources);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_CITATION_QUOTE, e.getMessage());
            }

            // Ground truth verification: batch chunk look-ups (one query, not one per source)
            Set<UUID> chunkIds = sources.stream()
                    .map(QuestionSource::getChunkId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<UUID, DocumentChunk> chunksById = documentChunkRepository.findAllByIds(chunkIds).stream()
                    .collect(Collectors.toMap(DocumentChunk::getChunkId, chunk -> chunk, (first, second) -> first));

            for (QuestionSource s : sources) {
                if (s.getChunkId() != null) {
                    DocumentChunk chunk = chunksById.get(s.getChunkId());
                    if (chunk != null) {
                        try {
                            s.validateCitationGrounding(chunk.getContent());
                        } catch (IllegalArgumentException e) {
                            throw new AppException(ErrorCode.INVALID_CITATION_QUOTE,
                                    "Đoạn trích dẫn không khớp với văn bản tài liệu gốc: " + s.getCitationQuote());
                        }
                    }
                }
            }
        }

        // 3. Mark version as APPROVED
        version.setApprovalStatus(QuestionApprovalStatus.APPROVED);
        version.setReviewedBy(lecturerId);
        version.setReviewedAt(Instant.now());
        questionVersionRepository.save(version);

        // 4. Supersede older approved versions
        questionVersionRepository.supersedeOlderApprovedVersions(questionId, versionId);

        log.info("Lecturer {} approved question {} version {}", lecturerId, questionId, versionId);
        return getQuestionDetails(questionId);
    }

    @Override
    @Transactional
    public QuestionDetailView rejectQuestionVersion(UUID questionId, UUID versionId, String reason, UUID lecturerId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

        if (!version.getQuestionId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (version.getApprovalStatus() != QuestionApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_NON_DRAFT);
        }

        version.setApprovalStatus(QuestionApprovalStatus.REJECTED);
        version.setRejectionReason(reason);
        version.setReviewedBy(lecturerId);
        version.setReviewedAt(Instant.now());
        questionVersionRepository.save(version);

        return getQuestionDetails(questionId);
    }

    @Override
    @Transactional
    public void deleteDraftVersion(UUID questionId, UUID versionId, UUID lecturerId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

        if (!version.getQuestionId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (version.getApprovalStatus() != QuestionApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_NON_DRAFT, "Chỉ có thể xóa phiên bản DRAFT");
        }

        // Remove rubrics and sources
        rubricRepository.findByQuestionVersionId(versionId).ifPresent(r -> {
            rubricCriterionRepository.deleteByRubricId(r.getRubricId());
            rubricRepository.deleteByQuestionVersionId(versionId);
        });
        questionSourceRepository.deleteByQuestionVersionId(versionId);

        questionVersionRepository.deleteById(versionId);

        // If no versions left, delete the Question parent
        List<QuestionVersion> remaining = questionVersionRepository.findByQuestionId(questionId);
        if (remaining.isEmpty()) {
            questionRepository.deleteById(questionId);
            log.info("Deleted question {} as its only version was deleted", questionId);
        }
    }

    @Override
    @Transactional
    public void archiveQuestion(UUID questionId, UUID lecturerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        questionRepository.updateStatus(questionId, QuestionStatus.ARCHIVED);
        log.info("Lecturer {} archived question {}", lecturerId, questionId);
    }

    @Override
    @Transactional
    public void restoreQuestion(UUID questionId, UUID lecturerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        questionRepository.updateStatus(questionId, QuestionStatus.ACTIVE);
        log.info("Lecturer {} restored question {}", lecturerId, questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<QuestionDetailView> searchQuestions(QuestionSearchCriteria criteria) {
        PagedResult<Question> pagedQuestions = questionRepository.search(criteria);
        List<Question> questions = pagedQuestions.getContent();

        List<QuestionDetailView> views = buildQuestionViews(questions);

        return PagedResult.<QuestionDetailView>builder()
                .content(views)
                .page(pagedQuestions.getPage())
                .size(pagedQuestions.getSize())
                .totalElements(pagedQuestions.getTotalElements())
                .totalPages(pagedQuestions.getTotalPages())
                .build();
    }

    /**
     * Batch-assembles detail views for a page of questions with 4 queries total
     * (versions, sources, rubrics, criteria) instead of ~6 queries per row (Rule 4: N+1).
     * In-memory derivation of active/draft/display version mirrors the ordering semantics
     * of the single-row queries (versionNumber descending).
     */
    private List<QuestionDetailView> buildQuestionViews(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        Map<UUID, List<QuestionVersion>> versionsByQuestion = questionVersionRepository.findByQuestionIds(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionVersion::getQuestionId));

        Set<UUID> allVersionIds = versionsByQuestion.values().stream()
                .flatMap(List::stream)
                .map(QuestionVersion::getQuestionVersionId)
                .collect(Collectors.toSet());

        Map<UUID, List<QuestionSource>> sourcesByVersion = questionSourceRepository.findByQuestionVersionIds(allVersionIds).stream()
                .collect(Collectors.groupingBy(QuestionSource::getQuestionVersionId));

        Map<UUID, Rubric> rubricByVersion = rubricRepository.findByQuestionVersionIds(allVersionIds).stream()
                .collect(Collectors.toMap(Rubric::getQuestionVersionId, rubric -> rubric, (first, second) -> first));

        Set<UUID> rubricIds = rubricByVersion.values().stream()
                .map(Rubric::getRubricId)
                .collect(Collectors.toSet());
        Map<UUID, List<RubricCriterion>> criteriaByRubric = rubricCriterionRepository.findByRubricIds(rubricIds).stream()
                .collect(Collectors.groupingBy(RubricCriterion::getRubricId));

        return questions.stream()
                .map(question -> {
                    List<QuestionVersion> versions = versionsByQuestion.getOrDefault(question.getQuestionId(), List.of()).stream()
                            .sorted(Comparator.comparing(QuestionVersion::getVersionNumber,
                                    Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                            .collect(Collectors.toList());

                    Optional<QuestionVersion> activeOpt = versions.stream()
                            .filter(v -> v.getApprovalStatus() == QuestionApprovalStatus.APPROVED)
                            .findFirst();
                    Optional<QuestionVersion> draftOpt = versions.stream()
                            .filter(v -> v.getApprovalStatus() == QuestionApprovalStatus.DRAFT)
                            .findFirst();

                    QuestionVersion displayVersion = activeOpt.or(() -> draftOpt)
                            .or(() -> versions.stream().findFirst()).orElse(null);

                    Rubric rubric = null;
                    List<RubricCriterion> criteria = Collections.emptyList();
                    List<QuestionSource> sources = Collections.emptyList();

                    if (displayVersion != null) {
                        rubric = rubricByVersion.get(displayVersion.getQuestionVersionId());
                        if (rubric != null) {
                            criteria = criteriaByRubric.getOrDefault(rubric.getRubricId(), Collections.emptyList());
                        }
                        sources = sourcesByVersion.getOrDefault(displayVersion.getQuestionVersionId(), Collections.emptyList());
                    }

                    return QuestionDetailView.builder()
                            .question(question)
                            .activeVersion(activeOpt.orElse(null))
                            .draftVersion(draftOpt.orElse(null))
                            .hasPendingDraft(draftOpt.isPresent())
                            .rubric(rubric)
                            .criteria(criteria)
                            .sources(sources)
                            .versionHistory(versions)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailView getQuestionDetails(UUID questionId) {
        return buildQuestionView(questionId);
    }

    private QuestionDetailView buildQuestionView(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        List<QuestionVersion> versions = questionVersionRepository.findByQuestionId(questionId);
        Optional<QuestionVersion> activeOpt = questionVersionRepository.findActiveApprovedVersion(questionId);
        Optional<QuestionVersion> draftOpt = questionVersionRepository.findLatestDraftVersion(questionId);

        // Display version: prefer active approved, fallback to draft
        QuestionVersion displayVersion = activeOpt.or(() -> draftOpt).or(() -> versions.stream().findFirst()).orElse(null);

        Rubric rubric = null;
        List<RubricCriterion> criteria = Collections.emptyList();
        List<QuestionSource> sources = Collections.emptyList();

        if (displayVersion != null) {
            Optional<Rubric> rOpt = rubricRepository.findByQuestionVersionId(displayVersion.getQuestionVersionId());
            if (rOpt.isPresent()) {
                rubric = rOpt.get();
                criteria = rubricCriterionRepository.findByRubricId(rubric.getRubricId());
            }
            sources = questionSourceRepository.findByQuestionVersionId(displayVersion.getQuestionVersionId());
        }

        return QuestionDetailView.builder()
                .question(question)
                .activeVersion(activeOpt.orElse(null))
                .draftVersion(draftOpt.orElse(null))
                .hasPendingDraft(draftOpt.isPresent())
                .rubric(rubric)
                .criteria(criteria)
                .sources(sources)
                .versionHistory(versions)
                .build();
    }
}
