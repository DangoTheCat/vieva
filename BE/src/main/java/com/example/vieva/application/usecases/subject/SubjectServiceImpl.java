package com.example.vieva.application.usecases.subject;

import com.example.vieva.application.ports.input.CreateSubjectRequest;
import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.application.ports.input.UpdateSubjectRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final AuditEventRepository auditEventRepository;
    private final com.example.vieva.application.ports.output.JsonSerializerPort jsonSerializer;

    @Override
    public Subject createSubject(CreateSubjectRequest request, UUID currentAdminId) {
        if (!StringUtils.hasText(request.getSubjectCode()) || !StringUtils.hasText(request.getSubjectName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedCode = request.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.existsBySubjectCode(normalizedCode)) {
            throw new AppException(ErrorCode.SUBJECT_CODE_EXISTED);
        }

        SubjectStatus status = request.getStatus() != null ? request.getStatus() : SubjectStatus.ACTIVE;
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

        Subject saved = subjectRepository.save(subject);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("SUBJECT_CREATED")
                .entityType("SUBJECT")
                .entityId(saved.getSubjectId().toString())
                .newValuesJson(jsonSerializer.serialize(java.util.Map.of(
                        "code", saved.getSubjectCode(),
                        "name", saved.getSubjectName(),
                        "status", saved.getStatus().name()
                )))
                .createdAt(Instant.now())
                .build());

        return saved;
    }

    @Override
    public Subject updateSubject(UUID subjectId, UpdateSubjectRequest request, UUID currentAdminId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        subject.update(request.getSubjectName(), request.getDescription(), request.getCredits(), request.getStatus());
        Subject updated = subjectRepository.save(subject);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("SUBJECT_UPDATED")
                .entityType("SUBJECT")
                .entityId(updated.getSubjectId().toString())
                .newValuesJson(jsonSerializer.serialize(java.util.Map.of(
                        "name", updated.getSubjectName(),
                        "status", updated.getStatus().name(),
                        "credits", updated.getCredits()
                )))
                .createdAt(Instant.now())
                .build());

        return updated;
    }

    @Override
    public void deactivateSubject(UUID subjectId, UUID currentAdminId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        subject.deactivate();
        subjectRepository.save(subject);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("SUBJECT_DEACTIVATED")
                .entityType("SUBJECT")
                .entityId(subject.getSubjectId().toString())
                .newValuesJson(jsonSerializer.serialize(java.util.Map.of("status", "INACTIVE")))
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Subject getSubjectById(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Subject> getSubjects(SubjectSearchCriteria criteria) {
        return subjectRepository.findAll(criteria);
    }
}
