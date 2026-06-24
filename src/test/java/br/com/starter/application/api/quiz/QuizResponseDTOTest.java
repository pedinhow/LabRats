package br.com.starter.application.api.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import br.com.starter.application.api.quiz.dto.QuizEditResponseDTO;
import br.com.starter.application.api.quiz.dto.QuizResponseDTO;
import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;

class QuizResponseDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void normalQuizResponseDoesNotExposeAnswerKey() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(QuizResponseDTO.fromQuiz(buildQuiz()));

        assertThat(json).doesNotContain("isCorrect");
        assertThat(json).doesNotContain("\"correct\"");
    }

    @Test
    void editingResponseKeepsAnswerKeyAvailableToOwnerEditor() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(QuizEditResponseDTO.fromQuiz(buildQuiz()));

        assertThat(json).contains("\"isCorrect\":true");
        assertThat(json).contains("\"isCorrect\":false");
    }

    private Quiz buildQuiz() {
        return new Quiz(
                "Quiz seguro",
                List.of(new Question(
                        "Pergunta",
                        List.of(
                                new Alternative("Certa", true),
                                new Alternative("Errada", false)
                        )
                ))
        );
    }
}
