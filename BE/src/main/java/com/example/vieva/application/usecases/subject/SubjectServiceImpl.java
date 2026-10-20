package com.example.vieva.application.usecases.subject;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.subject.CreateSubjectRequest;
import com.example.vieva.application.ports.input.subject.SubjectResult;
import com.example.vieva.application.ports.input.subject.SubjectSearchCriteria;
import com.example.vieva.application.ports.input.subject.SubjectService;
import com.example.vieva.application.ports.input.subject.UpdateSubjectRequest;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.ports.input.common.PageResult;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.common.EnumValues;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@UseCaseTransaction
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final QuestionBankAuditor auditor;

    @Override
    public SubjectResult createSubject(CreateSubjectRequest request, UUID currentAdminId) {
        if (request.getSubjectCode() == null || request.getSubjectCode().isBlank()
                || request.getSubjectName() == null || request.getSubjectName().isBlank()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedCode = request.getSubjectCode().trim().toUpperCase(Locale.ROOT);
        if (subjectRepository.existsBySubjectCode(normalizedCode)) {
            throw new ApplicationException(ErrorCode.SUBJECT_CODE_EXISTED);
        }

        SubjectStatus parsedStatus = EnumValues.parse(SubjectStatus.class, request.getStatus(), "Malformed request body");
        SubjectStatus status = parsedStatus != null ? parsedStatus : SubjectStatus.ACTIVE;
        Integer credits = request.getCredits() != null && request.getCredits() > 0 ? request.getCredits() : 3;

        Subject subject = Subject.builder()
                .subjectId(UUID.randomUUID())
                .subjectCode(normalizedCode)
                .subjectName(request.getSubjectName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .credits(credits)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        final Subject saved;
        try {
            // Flush forces the INSERT now so a concurrent duplicate surfaces here (unique index)
            saved = subjectRepository.saveAndFlush(subject);
        } catch (DuplicateKeyException e) {
            // Concurrent creation with the same code won the race (unique index)
            throw new ApplicationException(ErrorCode.SUBJECT_CODE_EXISTED);
        }

        Map<String, Object> auditValues = new HashMap<>();
        auditValues.put("code", saved.getSubjectCode());
        auditValues.put("name", saved.getSubjectName());
        auditValues.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);

        auditor.record(currentAdminId, "SUBJECT_CREATED", "SUBJECT", saved.getSubjectId(), auditValues);

        return SubjectResults.from(saved);
    }

    @Override
    public SubjectResult updateSubject(UUID subjectId, UpdateSubjectRequest request, UUID currentAdminId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectStatus parsedStatus = EnumValues.parse(SubjectStatus.class, request.getStatus(), "Malformed request body");
        subject.update(request.getSubjectName(), request.getDescription(), request.getCredits(), parsedStatus);
        Subject updated = subjectRepository.save(subject);

        Map<String, Object> auditValues = new HashMap<>();
        auditValues.put("name", updated.getSubjectName());
        auditValues.put("status", updated.getStatus() != null ? updated.getStatus().name() : null);
        auditValues.put("credits", updated.getCredits());

        auditor.record(currentAdminId, "SUBJECT_UPDATED", "SUBJECT", updated.getSubjectId(), auditValues);

        return SubjectResults.from(updated);
    }

    @Override
    public void deactivateSubject(UUID subjectId, UUID currentAdminId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));

        subject.deactivate();
        subjectRepository.save(subject);

        Map<String, Object> auditValues = new HashMap<>();
        auditValues.put("status", SubjectStatus.INACTIVE.name());

        auditor.record(currentAdminId, "SUBJECT_DEACTIVATED", "SUBJECT", subject.getSubjectId(), auditValues);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public SubjectResult getSubjectById(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));
        return SubjectResults.from(subject);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public PageResult<SubjectResult> getSubjects(SubjectSearchCriteria criteria) {
        if (criteria != null && criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            EnumValues.parse(SubjectStatus.class, criteria.getStatus(), "Invalid value for parameter 'status'");
        }
        PageResult<Subject> paged = subjectRepository.findAll(criteria);
        List<SubjectResult> content = paged.getContent().stream()
                .map(SubjectResults::from)
                .toList();
        return PageResult.<SubjectResult>builder()
                .content(content)
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .isFirst(paged.isFirst())
                .isLast(paged.isLast())
                .build();
    }
}
