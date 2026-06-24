package br.com.starter.application.api.quizattempt.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.starter.domain.quizattempt.QuizAttempt;
import br.com.starter.domain.quizattempt.UserAnswer;

public record QuizAttemptResultResponseDTO(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        int score,
        int totalQuestions,
        int percentage,
        LocalDateTime completedAt,
        List<AnswerResultDTO> answers
) {
    public static QuizAttemptResultResponseDTO fromAttempt(QuizAttempt attempt) {
        int totalQuestions = attempt.getTotalQuestions();
        int score = attempt.getScore();
        int percentage = totalQuestions == 0 ? 0 : Math.round((score * 100f) / totalQuestions);

        return new QuizAttemptResultResponseDTO(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                score,
                totalQuestions,
                percentage,
                attempt.getCompletedAt(),
                attempt.getAnswers().stream().map(AnswerResultDTO::fromAnswer).toList()
        );
    }

    public record AnswerResultDTO(
            UUID questionId,
            String statement,
            AlternativeResultDTO selectedAlternative,
            AlternativeResultDTO correctAlternative,
            boolean correct
    ) {
        private static AnswerResultDTO fromAnswer(UserAnswer answer) {
            return new AnswerResultDTO(
                    answer.getQuestionId(),
                    answer.getQuestionStatement(),
                    answer.getSelectedAlternativeId() == null
                            ? null
                            : new AlternativeResultDTO(
                                    answer.getSelectedAlternativeId(),
                                    answer.getSelectedAlternativeText()
                            ),
                    new AlternativeResultDTO(
                            answer.getCorrectAlternativeId(),
                            answer.getCorrectAlternativeText()
                    ),
                    answer.isCorrect()
            );
        }
    }

    public record AlternativeResultDTO(UUID id, String text) {
    }
}
