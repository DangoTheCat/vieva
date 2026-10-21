package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.Rubric;

import java.util.List;

public interface AttemptGradingGateway {
    AttemptGradingResult gradeAttempt(ExamAttempt attempt, List<ExamTurn> turns, Rubric rubric);
}

