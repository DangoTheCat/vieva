package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamTurn;
import org.springframework.stereotype.Component;

@Component
public class ExamTurnPersistenceMapper {

    public ExamTurnJpaEntity toEntity(ExamTurn domain) {
        if (domain == null) {
            return null;
        }
        return ExamTurnJpaEntity.builder()
                .id(domain.getId())
                .attemptId(domain.getAttemptId())
                .questionId(domain.getQuestionId())
                .questionVersionId(domain.getQuestionVersionId())
                .turnType(domain.getTurnType())
                .parentTurnId(domain.getParentTurnId())
                .turnOrder(domain.getTurnOrder())
                .questionText(domain.getQuestionText())
                .ttsAudioUrl(domain.getTtsAudioUrl())
                .studentAudioUrl(domain.getStudentAudioUrl())
                .transcriptText(domain.getTranscriptText())
                .status(domain.getStatus())
                .score(domain.getScore())
                .feedback(domain.getFeedback())
                .rubricEvaluations(domain.getRubricEvaluations())
                .teacherScore(domain.getTeacherScore())
                .teacherFeedback(domain.getTeacherFeedback())
                .teacherId(domain.getTeacherId())
                .teacherGradedAt(domain.getTeacherGradedAt())
                .askedAt(domain.getAskedAt())
                .timedOut(domain.isTimedOut())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNewEntity(false)
                .build();
    }

    public ExamTurn toDomain(ExamTurnJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExamTurn.builder()
                .id(entity.getId())
                .attemptId(entity.getAttemptId())
                .questionId(entity.getQuestionId())
                .questionVersionId(entity.getQuestionVersionId())
                .turnType(entity.getTurnType())
                .parentTurnId(entity.getParentTurnId())
                .turnOrder(entity.getTurnOrder())
                .questionText(entity.getQuestionText())
                .ttsAudioUrl(entity.getTtsAudioUrl())
                .studentAudioUrl(entity.getStudentAudioUrl())
                .transcriptText(entity.getTranscriptText())
                .status(entity.getStatus())
                .score(entity.getScore())
                .feedback(entity.getFeedback())
                .rubricEvaluations(entity.getRubricEvaluations())
                .teacherScore(entity.getTeacherScore())
                .teacherFeedback(entity.getTeacherFeedback())
                .teacherId(entity.getTeacherId())
                .teacherGradedAt(entity.getTeacherGradedAt())
                .askedAt(entity.getAskedAt())
                .timedOut(entity.isTimedOut())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
