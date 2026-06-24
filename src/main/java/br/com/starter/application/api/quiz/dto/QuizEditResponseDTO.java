package br.com.starter.application.api.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;

public record QuizEditResponseDTO(
        UUID id,
        String title,
        LocalDateTime createdAt,
        List<QuestionEditResponseDTO> questions
) {

    public static QuizEditResponseDTO fromQuiz(Quiz quiz) {
        return new QuizEditResponseDTO(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCreatedAt(),
                quiz.getQuestions() == null ? List.of() : quiz.getQuestions().stream()
                        .map(QuestionEditResponseDTO::fromQuestion)
                        .toList()
        );
    }

    public record QuestionEditResponseDTO(
            UUID id,
            String statement,
            Integer durationSeconds,
            List<AlternativeEditResponseDTO> alternatives
    ) {
        public static QuestionEditResponseDTO fromQuestion(Question question) {
            return new QuestionEditResponseDTO(
                    question.getId(),
                    question.getStatement(),
                    question.getDurationSeconds(),
                    question.getAlternatives() == null ? List.of() : question.getAlternatives().stream()
                            .map(AlternativeEditResponseDTO::fromAlternative)
                            .toList()
            );
        }
    }

    public record AlternativeEditResponseDTO(UUID id, String text, Boolean isCorrect) {
        public static AlternativeEditResponseDTO fromAlternative(Alternative alternative) {
            return new AlternativeEditResponseDTO(
                    alternative.getId(),
                    alternative.getText(),
                    alternative.getIsCorrect()
            );
        }
    }
}
