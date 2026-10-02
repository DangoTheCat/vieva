package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionVersionRepositoryImpl implements QuestionVersionRepository {

    private final QuestionVersionJpaRepository jpaRepository;
    private final QuestionVersionPersistenceMapper mapper;

    @Override
    public QuestionVersion save(QuestionVersion version) {
        QuestionVersionJpaEntity entity = mapper.toEntity(version);
        QuestionVersionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<QuestionVersion> saveAll(List<QuestionVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        List<QuestionVersionJpaEntity> entities = versions.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<QuestionVersion> findById(UUID versionId) {
        return jpaRepository.findById(versionId)
                .map(mapper::toDomain);
    }

    @Override
    public List<QuestionVersion> findByQuestionId(UUID questionId) {
        return jpaRepository.findByQuestion_QuestionIdOrderByVersionNumberDesc(questionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVersion> findByQuestionIds(Collection<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByQuestion_QuestionIdIn(questionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<QuestionVersion> findActiveApprovedVersion(UUID questionId) {
        return jpaRepository.findFirstByQuestion_QuestionIdAndApprovalStatusOrderByVersionNumberDesc(
                questionId, QuestionApprovalStatus.APPROVED)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<QuestionVersion> findLatestDraftVersion(UUID questionId) {
        return jpaRepository.findFirstByQuestion_QuestionIdAndApprovalStatusOrderByVersionNumberDesc(
                questionId, QuestionApprovalStatus.DRAFT)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<QuestionVersion> findLatestVersion(UUID questionId) {
        return jpaRepository.findFirstByQuestion_QuestionIdOrderByVersionNumberDesc(questionId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean hasDraftVersion(UUID questionId) {
        return jpaRepository.existsByQuestion_QuestionIdAndApprovalStatus(questionId, QuestionApprovalStatus.DRAFT);
    }

    @Override
    public void supersedeOlderApprovedVersions(UUID questionId, UUID currentApprovedVersionId) {
        jpaRepository.supersedeOlderApprovedVersions(questionId, currentApprovedVersionId);
    }

    @Override
    public void deleteById(UUID versionId) {
        jpaRepository.deleteById(versionId);
    }
}
