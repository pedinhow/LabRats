package br.com.starter.application.api.quizattempt.dto;

import java.util.UUID;

import br.com.starter.application.api.quiz.dto.QuizResponseDTO;
import br.com.starter.domain.quizattempt.QuizAttempt;

public record StartQuizAttemptResponseDTO(
        UUID attemptId,
        QuizResponseDTO quiz
) {
    public static StartQuizAttemptResponseDTO fromAttempt(QuizAttempt attempt) {
        return new StartQuizAttemptResponseDTO(
                attempt.getId(),
                QuizResponseDTO.fromQuiz(attempt.getQuiz())
        );
    }
}
