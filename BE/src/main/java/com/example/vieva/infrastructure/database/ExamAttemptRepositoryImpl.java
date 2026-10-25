package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final ExamAttemptJpaRepository jpaRepository;
    private final ExamAttemptPersistenceMapper mapper;

    @Override
    public ExamAttempt save(ExamAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        boolean exists = attempt.getId() != null && jpaRepository.existsById(attempt.getId());
        ExamAttemptJpaEntity entity = mapper.toEntity(attempt);
        entity.setNewEntity(!exists);
        ExamAttemptJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExamAttempt> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ExamAttempt> findByIdForUpdate(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<ExamAttempt> findByStudentAndStatus(UUID studentId, AttemptStatus status) {
        if (studentId == null || status == null) {
            return List.of();
        }
        return jpaRepository.findByStudentIdAndStatus(studentId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamAttempt> findByStatus(AttemptStatus status) {
        if (status == null) {
            return List.of();
        }
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamAttempt> findByStudentId(UUID studentId) {
        if (studentId == null) {
            return List.of();
        }
        return jpaRepository.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId, UUID studentId) {
        if (examId == null || studentId == null) {
            return Optional.empty();
        }
        return jpaRepository.findByExamIdAndStudentId(examId, studentId).map(mapper::toDomain);
    }

    @Override
    public long countByExamId(UUID examId) {
        if (examId == null) {
            return 0;
        }
        return jpaRepository.countByExamId(examId);
    }

    @Override
    public List<ExamAttempt> findByExamId(UUID examId) {
        if (examId == null) {
            return List.of();
        }
        return jpaRepository.findByExamId(examId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(AttemptStatus status) {
        if (status == null) {
            return 0;
        }
        return jpaRepository.countByStatus(status);
    }

    @Override
    public PageResult<ExamAttempt> findCompletedAttempts(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        Page<ExamAttemptJpaEntity> entityPage = jpaRepository.findByStatusOrderByCompletedAtDesc(
                AttemptStatus.COMPLETED, PageRequest.of(safePage, safeSize)
        );
        List<ExamAttempt> content = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return PageResult.of(content, entityPage.getNumber(), entityPage.getSize(), entityPage.getTotalElements());
    }

    @Override
    public PageResult<ExamAttempt> findAttemptsWithFilters(
            UUID studentId,
            UUID subjectId,
            UUID examId,
            AttemptStatus status,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);

        Specification<ExamAttemptJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subjectId"), subjectId));
            }
            if (examId != null) {
                predicates.add(cb.equal(root.get("examId"), examId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ExamAttemptJpaEntity> entityPage = jpaRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ExamAttempt> content = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return PageResult.of(content, entityPage.getNumber(), entityPage.getSize(), entityPage.getTotalElements());
    }
}
