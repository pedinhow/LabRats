package br.com.starter.application.api.quizattempt.dto;

import java.util.List;
import java.util.UUID;

public record SubmitQuizAttemptRequestDTO(List<AnswerRequestDTO> answers) {
    public record AnswerRequestDTO(UUID questionId, UUID alternativeId) {
    }
}
