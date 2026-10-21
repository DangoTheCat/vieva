package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;

import java.util.List;

public interface AttemptGradingService {
    ExamScore gradeAndCompleteAttempt(ExamAttempt attempt, List<ExamTurn> turns);
}
