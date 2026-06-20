package br.com.starter.application.api.quiz.dto;

import java.util.List;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;

public record QuizResponseDTO(String title, List<QuestionResponseDTO> questions) {

    public static QuizResponseDTO fromQuiz(Quiz quiz) {
        return new QuizResponseDTO(
                quiz.getTitle(),
                quiz.getQuestions() == null ? List.of() : quiz.getQuestions().stream()
                        .map(QuestionResponseDTO::fromQuestion)
                        .toList()
        );
    }

    public record QuestionResponseDTO(String statement, List<AlternativeResponseDTO> alternatives) {
        public static QuestionResponseDTO fromQuestion(Question question) {
            return new QuestionResponseDTO(
                    question.getStatement(),
                    question.getAlternatives() == null ? List.of() : question.getAlternatives().stream()
                            .map(AlternativeResponseDTO::fromAlternative)
                            .toList()
            );
        }
    }

    public record AlternativeResponseDTO(String text, Boolean isCorrect) {
        public static AlternativeResponseDTO fromAlternative(Alternative alternative) {
            return new AlternativeResponseDTO(alternative.getText(), alternative.getIsCorrect());
        }
    }
}
