package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.input.exam.ExamAttemptDetailView;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.ExamTurnDetailView;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.input.exam.TurnReadyView;
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
            Instant pausedAt,
            Integer remainingSeconds,
            Instant graceDeadline,
            UUID currentTurnId,
            String currentQuestionText,
            String currentTtsAudioUrl,
            Integer answerTimeLimitSeconds,
            Integer maxFollowUpsPerQuestion
    ) {
        public ExamAttemptResponse(
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
                Instant pausedAt,
                Integer remainingSeconds,
                Instant graceDeadline,
                UUID currentTurnId,
                String currentQuestionText,
                String currentTtsAudioUrl
        ) {
            this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                    startedAt, completedAt, pausedAt, remainingSeconds, graceDeadline, currentTurnId,
                    currentQuestionText, currentTtsAudioUrl, null, null);
        }

        public ExamAttemptResponse(
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
        ) {
            this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                    startedAt, completedAt, null, null, null, currentTurnId, currentQuestionText, currentTtsAudioUrl);
        }
    }

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
            Instant updatedAt,
            Instant askedAt,
            boolean timedOut
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
            Instant pausedAt,
            Integer remainingSeconds,
            Instant graceDeadline,
            String detailedFeedback,
            String scoringMode,
            List<ExamTurnDetailResponse> turns,
            Integer answerTimeLimitSeconds,
            Integer maxFollowUpsPerQuestion
    ) {
        public ExamAttemptDetailResponse(
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
                Instant pausedAt,
                Integer remainingSeconds,
                Instant graceDeadline,
                String detailedFeedback,
                String scoringMode,
                List<ExamTurnDetailResponse> turns
        ) {
            this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                    startedAt, completedAt, pausedAt, remainingSeconds, graceDeadline, detailedFeedback, scoringMode,
                    turns, null, null);
        }

        public ExamAttemptDetailResponse(
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
        ) {
            this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                    startedAt, completedAt, null, null, null, detailedFeedback, scoringMode, turns);
        }
    }

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
            Instant updatedAt,
            Integer remainingSeconds,
            boolean timedOut
    ) {}

    public record TurnReadyResponse(
            UUID turnId,
            UUID attemptId,
            Instant askedAt,
            Instant answerDeadline,
            int answerTimeLimitSeconds,
            int remainingSeconds,
            Instant serverTime
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
                view.pausedAt(),
                view.remainingSeconds(),
                view.graceDeadline(),
                view.currentTurnId(),
                view.currentQuestionText(),
                view.currentTtsAudioUrl(),
                view.answerTimeLimitSeconds(),
                view.maxFollowUpsPerQuestion()
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
                view.updatedAt(),
                view.askedAt(),
                view.timedOut()
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
                view.pausedAt(),
                view.remainingSeconds(),
                view.graceDeadline(),
                view.detailedFeedback(),
                view.scoringMode(),
                turnResponses,
                view.answerTimeLimitSeconds(),
                view.maxFollowUpsPerQuestion()
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
                view.updatedAt(),
                view.remainingSeconds(),
                view.timedOut()
        );
    }

    public TurnReadyResponse toTurnReadyResponse(TurnReadyView view) {
        if (view == null) return null;
        return new TurnReadyResponse(
                view.turnId(),
                view.attemptId(),
                view.askedAt(),
                view.answerDeadline(),
                view.answerTimeLimitSeconds(),
                view.remainingSeconds(),
                view.serverTime()
        );
    }
}
