package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;

/**
 * Students only see a score, grade or AI feedback after the lecturer has finalized ("chốt") the
 * grade. Until then every student-facing view hides them, even though the AI has already graded.
 */
final class StudentScoreVisibility {

    private StudentScoreVisibility() {
    }

    static boolean isReleased(ExamScore score) {
        return score != null && score.isPublished();
    }

    static boolean isReleased(ExamScoreRepository examScoreRepository, ExamAttempt attempt) {
        if (examScoreRepository == null || attempt == null || attempt.getId() == null) {
            return false;
        }
        return isReleased(examScoreRepository.findByAttemptId(attempt.getId()).orElse(null));
    }
}
