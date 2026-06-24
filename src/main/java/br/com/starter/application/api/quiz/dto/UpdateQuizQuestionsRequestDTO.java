package br.com.starter.application.api.quiz.dto;

import java.util.List;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;

public record UpdateQuizQuestionsRequestDTO(List<QuestionRequestDTO> questions) {
    public List<Question> toQuestions() {
        return questions == null
                ? null
                : questions.stream()
                        .map(question -> question == null ? null : question.toQuestion())
                        .toList();
    }

    public record QuestionRequestDTO(
            String statement,
            Integer durationSeconds,
            List<AlternativeRequestDTO> alternatives
    ) {
        private Question toQuestion() {
            List<Alternative> mappedAlternatives = alternatives == null
                    ? null
                    : alternatives.stream()
                            .map(alternative -> alternative == null
                                    ? new Alternative(null, false)
                                    : alternative.toAlternative())
                            .toList();
            Question question = new Question(statement, mappedAlternatives);
            question.setDurationSeconds(durationSeconds);
            return question;
        }
    }

    public record AlternativeRequestDTO(String text, Boolean isCorrect) {
        private Alternative toAlternative() {
            return new Alternative(text, isCorrect);
        }
    }
}
