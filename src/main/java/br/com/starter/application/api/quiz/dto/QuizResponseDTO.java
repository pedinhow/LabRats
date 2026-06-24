package br.com.starter.application.api.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;

public record QuizResponseDTO(
        UUID id,
        String title,
        LocalDateTime createdAt,
        List<QuestionResponseDTO> questions
) {

    public static QuizResponseDTO fromQuiz(Quiz quiz) {
        return new QuizResponseDTO(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCreatedAt(),
                quiz.getQuestions() == null ? List.of() : quiz.getQuestions().stream()
                        .map(QuestionResponseDTO::fromQuestion)
                        .toList()
        );
    }

    public record QuestionResponseDTO(UUID id, String statement, List<AlternativeResponseDTO> alternatives) {
        public static QuestionResponseDTO fromQuestion(Question question) {
            return new QuestionResponseDTO(
                    question.getId(),
                    question.getStatement(),
                    question.getAlternatives() == null ? List.of() : question.getAlternatives().stream()
                            .map(AlternativeResponseDTO::fromAlternative)
                            .toList()
            );
        }
    }

    public record AlternativeResponseDTO(UUID id, String text) {
        public static AlternativeResponseDTO fromAlternative(Alternative alternative) {
            return new AlternativeResponseDTO(
                    alternative.getId(),
                    alternative.getText()
            );
        }
    }
}
