package br.com.starter.application.api.quizattempt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.com.starter.application.api.quizattempt.dto.QuizAttemptResultResponseDTO;
import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quizattempt.QuizAttempt;
import br.com.starter.domain.quizattempt.UserAnswer;
import br.com.starter.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class QuizAttemptResultResponseDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void resultContainsScoreAndAnswerKey() throws Exception {
        User participant = new User();
        Quiz quiz = new Quiz(
                "Quiz",
                List.of(new Question(
                        "Pergunta",
                        List.of(
                                new Alternative("Correta", true),
                                new Alternative("Errada", false)
                        )
                ))
        );
        QuizAttempt attempt = new QuizAttempt(quiz, participant, 1);
        attempt.complete(List.of(new UserAnswer(
                null,
                "Pergunta",
                null,
                "Errada",
                null,
                "Correta",
                false
        )), 0);

        String json = objectMapper.writeValueAsString(QuizAttemptResultResponseDTO.fromAttempt(attempt));

        assertThat(json).contains("\"score\":0");
        assertThat(json).contains("\"correctAlternative\"");
        assertThat(json).contains("\"text\":\"Correta\"");
        assertThat(json).contains("\"correct\":false");
    }
}
