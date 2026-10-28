package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamAttemptDetailView;
import com.example.vieva.application.ports.input.exam.ExamTurnDetailView;
import com.example.vieva.application.ports.input.exam.GetExamAttemptUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GetExamAttemptUseCaseImpl implements GetExamAttemptUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamScoreRepository examScoreRepository;

    @Override
    @UseCaseTransaction(readOnly = true)
    public ExamAttemptDetailView getAttemptDetail(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }

        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        ExamScore score = examScoreRepository.findByAttemptId(attemptId).orElse(null);

        List<ExamTurnDetailView> turnViews = turns.stream()
                .map(t -> new ExamTurnDetailView(
                        t.getId(),
                        t.getAttemptId(),
                        t.getQuestionId(),
                        t.getQuestionVersionId(),
                        t.getTurnType().name(),
                        t.getParentTurnId(),
                        t.getTurnOrder(),
                        t.getQuestionText(),
                        t.getTtsAudioUrl(),
                        t.getStudentAudioUrl(),
                        t.getTranscriptText(),
                        t.getStatus().name(),
                        t.getScore(),
                        t.getFeedback(),
                        t.getRubricEvaluations(),
                        t.getCreatedAt(),
                        t.getUpdatedAt()
                ))
                .toList();

        return new ExamAttemptDetailView(
                attempt.getId(),
                attempt.getStudentId(),
                attempt.getSubjectId(),
                attempt.getStatus().name(),
                attempt.getCurrentQuestionIndex(),
                attempt.getTotalQuestions(),
                attempt.getTotalScore(),
                attempt.getGradeLevel() != null ? attempt.getGradeLevel().name() : null,
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getPausedAt(),
                attempt.remainingSecondsAt(Instant.now()),
                attempt.getGraceDeadline(),
                score != null ? score.getDetailedFeedback() : null,
                score != null && score.getScoringMode() != null ? score.getScoringMode().name() : null,
                turnViews
        );
    }
}
