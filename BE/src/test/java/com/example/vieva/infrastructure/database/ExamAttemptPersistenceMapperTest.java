package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.GradeLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExamAttemptPersistenceMapperTest {

    private final ExamAttemptPersistenceMapper mapper = new ExamAttemptPersistenceMapper();

    @Test
    @DisplayName("toEntity and toDomain map all fields correctly including pause/timer attributes")
    void bidirectionalMapping_preservesAllFields() {
        Instant now = Instant.now();
        Instant pausedAt = now.minusSeconds(30);
        Instant graceDeadline = now.plusSeconds(90);
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        ExamAttempt domain = ExamAttempt.builder()
                .id(attemptId)
                .studentId(studentId)
                .subjectId(subjectId)
                .status(AttemptStatus.RECONNECTING)
                .currentQuestionIndex(2)
                .totalQuestions(5)
                .totalScore(BigDecimal.valueOf(8.50))
                .gradeLevel(GradeLevel.GOOD)
                .startedAt(now.minusSeconds(300))
                .completedAt(null)
                .pausedAt(pausedAt)
                .remainingSeconds(450)
                .graceDeadline(graceDeadline)
                .createdAt(now.minusSeconds(350))
                .updatedAt(now)
                .build();

        ExamAttemptJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(attemptId);
        assertThat(entity.getStudentId()).isEqualTo(studentId);
        assertThat(entity.getSubjectId()).isEqualTo(subjectId);
        assertThat(entity.getStatus()).isEqualTo(AttemptStatus.RECONNECTING);
        assertThat(entity.getCurrentQuestionIndex()).isEqualTo(2);
        assertThat(entity.getTotalQuestions()).isEqualTo(5);
        assertThat(entity.getTotalScore()).isEqualTo(BigDecimal.valueOf(8.50));
        assertThat(entity.getGradeLevel()).isEqualTo(GradeLevel.GOOD);
        assertThat(entity.getStartedAt()).isEqualTo(domain.getStartedAt());
        assertThat(entity.getCompletedAt()).isNull();
        assertThat(entity.getPausedAt()).isEqualTo(pausedAt);
        assertThat(entity.getRemainingSeconds()).isEqualTo(450);
        assertThat(entity.getGraceDeadline()).isEqualTo(graceDeadline);
        assertThat(entity.isNew()).isFalse();

        ExamAttempt roundTripDomain = mapper.toDomain(entity);

        assertThat(roundTripDomain).isNotNull();
        assertThat(roundTripDomain.getId()).isEqualTo(attemptId);
        assertThat(roundTripDomain.getStudentId()).isEqualTo(studentId);
        assertThat(roundTripDomain.getSubjectId()).isEqualTo(subjectId);
        assertThat(roundTripDomain.getStatus()).isEqualTo(AttemptStatus.RECONNECTING);
        assertThat(roundTripDomain.getPausedAt()).isEqualTo(pausedAt);
        assertThat(roundTripDomain.getRemainingSeconds()).isEqualTo(450);
        assertThat(roundTripDomain.getGraceDeadline()).isEqualTo(graceDeadline);
    }

    @Test
    @DisplayName("null domain or entity returns null")
    void handlesNullSafely() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
