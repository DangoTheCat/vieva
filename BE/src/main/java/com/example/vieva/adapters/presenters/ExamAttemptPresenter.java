package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.input.exam.ExamAttemptDetailView;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.ExamTurnDetailView;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ExamAttemptPresenter {

    public record ExamAttemptResponse(
            UUID attemptId,
            UUID studentId,
            UUID subjectId,
            String status,
            int currentQuestionIndex,
            int totalQuestions,
            BigDecimal totalScore,
            String gradeLevel,
            Instant startedAt,
            Instant completedAt,
            UUID currentTurnId,
            String currentQuestionText,
            String currentTtsAudioUrl
    ) {}

    public record ExamTurnDetailResponse(
            UUID turnId,
            UUID attemptId,
            UUID questionId,
            UUID questionVersionId,
            String turnType,
            UUID parentTurnId,
            int turnOrder,
            String questionText,
            String ttsAudioUrl,
            String studentAudioUrl,
            String transcriptText,
            String status,
            BigDecimal score,
            String feedback,
            String rubricEvaluations,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ExamAttemptDetailResponse(
            UUID attemptId,
            UUID studentId,
            UUID subjectId,
            String status,
            int currentQuestionIndex,
            int totalQuestions,
            BigDecimal totalScore,
            String gradeLevel,
            Instant startedAt,
            Instant completedAt,
            String detailedFeedback,
            String scoringMode,
            List<ExamTurnDetailResponse> turns
    ) {}

    public record TurnEvaluationResponse(
            UUID turnId,
            UUID attemptId,
            String turnType,
            int turnOrder,
            String status,
            BigDecimal score,
            String feedback,
            String rubricEvaluations,
            String studentAudioUrl,
            String transcriptText,
            boolean isAttemptFinished,
            UUID nextTurnId,
            String nextQuestionText,
            String nextTurnType,
            Instant updatedAt
    ) {}

    public ExamAttemptResponse toAttemptResponse(ExamAttemptView view) {
        if (view == null) return null;
        return new ExamAttemptResponse(
                view.attemptId(),
                view.studentId(),
                view.subjectId(),
                view.status(),
                view.currentQuestionIndex(),
                view.totalQuestions(),
                view.totalScore(),
                view.gradeLevel(),
                view.startedAt(),
                view.completedAt(),
                view.currentTurnId(),
                view.currentQuestionText(),
                view.currentTtsAudioUrl()
        );
    }

    public ExamTurnDetailResponse toTurnDetailResponse(ExamTurnDetailView view) {
        if (view == null) return null;
        return new ExamTurnDetailResponse(
                view.turnId(),
                view.attemptId(),
                view.questionId(),
                view.questionVersionId(),
                view.turnType(),
                view.parentTurnId(),
                view.turnOrder(),
                view.questionText(),
                view.ttsAudioUrl(),
                view.studentAudioUrl(),
                view.transcriptText(),
                view.status(),
                view.score(),
                view.feedback(),
                view.rubricEvaluations(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ExamAttemptDetailResponse toAttemptDetailResponse(ExamAttemptDetailView view) {
        if (view == null) return null;
        List<ExamTurnDetailResponse> turnResponses = view.turns() != null
                ? view.turns().stream().map(this::toTurnDetailResponse).toList()
                : Collections.emptyList();

        return new ExamAttemptDetailResponse(
                view.attemptId(),
                view.studentId(),
                view.subjectId(),
                view.status(),
                view.currentQuestionIndex(),
                view.totalQuestions(),
                view.totalScore(),
                view.gradeLevel(),
                view.startedAt(),
                view.completedAt(),
                view.detailedFeedback(),
                view.scoringMode(),
                turnResponses
        );
    }

    public TurnEvaluationResponse toTurnEvaluationResponse(TurnEvaluationView view) {
        if (view == null) return null;
        return new TurnEvaluationResponse(
                view.turnId(),
                view.attemptId(),
                view.turnType(),
                view.turnOrder(),
                view.status(),
                view.score(),
                view.feedback(),
                view.rubricEvaluations(),
                view.studentAudioUrl(),
                view.transcriptText(),
                view.isAttemptFinished(),
                view.nextTurnId(),
                view.nextQuestionText(),
                view.nextTurnType(),
                view.updatedAt()
        );
    }
}
