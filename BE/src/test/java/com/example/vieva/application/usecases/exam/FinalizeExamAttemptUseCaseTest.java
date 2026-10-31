package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.input.exam.FinalizeExamAttemptUseCase.Outcome;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeExamAttemptUseCaseTest {

    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private ExamTurnRepository examTurnRepository;
    @Mock private ExamScoreRepository examScoreRepository;
    @Mock private AttemptGradingService attemptGradingService;

    private FinalizeExamAttemptUseCaseImpl useCase;
    private ExamAttempt attempt;

    @BeforeEach
    void setUp() {
        useCase = new FinalizeExamAttemptUseCaseImpl(examAttemptRepository, examTurnRepository, examScoreRepository,
                attemptGradingService);
        attempt = ExamAttempt.init(UUID.randomUUID(), UUID.randomUUID(), 1);
        attempt.start(true);
        attempt.finish();
    }

    @Test
    void gradesFinalizingAttempt() {
        when(examAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attempt.getId())).thenReturn(List.of());
        when(attemptGradingService.gradeAndCompleteAttempt(any(), any())).thenReturn(mock(ExamScore.class));

        assertThat(useCase.finalizeAttempt(attempt.getId())).isEqualTo(Outcome.COMPLETED);
    }

    @Test
    void existingScoreOnlyReconcilesStatus() {
        when(examAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attempt.getId())).thenReturn(Optional.of(mock(ExamScore.class)));

        assertThat(useCase.finalizeAttempt(attempt.getId())).isEqualTo(Outcome.ALREADY_COMPLETED);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        verifyNoInteractions(attemptGradingService);
    }

    @Test
    void missingAttemptIsReported() {
        UUID id = UUID.randomUUID();
        when(examAttemptRepository.findById(id)).thenReturn(Optional.empty());
        assertThat(useCase.finalizeAttempt(id)).isEqualTo(Outcome.NOT_FOUND);
    }

    @Test
    void markFailedInterruptsFinalizingAttempt() {
        when(examAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        useCase.markFinalizationFailed(attempt.getId());
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        verify(examAttemptRepository).save(attempt);
    }
}
