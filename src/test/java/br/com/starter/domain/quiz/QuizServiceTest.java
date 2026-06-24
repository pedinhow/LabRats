package br.com.starter.domain.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.com.starter.domain.user.User;

class QuizServiceTest {

    @Test
    void generatesAssociatesAndSavesQuizForAuthenticatedUser() {
        User creator = new User();
        Quiz generatedQuiz = new Quiz(
                "Titulo retornado pela IA",
                List.of(new Question(
                        "Pergunta gerada",
                        List.of(
                                new Alternative("Correta", true),
                                new Alternative("Incorreta", false)
                        )
                ))
        );

        QuizGeneratorClient generatorClient = baseText -> generatedQuiz;
        QuizRepository quizRepository = mock(QuizRepository.class);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizService service = new QuizService(generatorClient, quizRepository);

        Quiz savedQuiz = service.generate("  sistema solar e planetas  ", creator);

        assertThat(savedQuiz.getCreator()).isSameAs(creator);
        assertThat(savedQuiz.getTitle()).isEqualTo("Quiz sistema solar");
        assertThat(savedQuiz.getQuestions()).hasSize(1);
        assertThat(savedQuiz.getQuestions().get(0).getQuiz()).isSameAs(savedQuiz);
        assertThat(savedQuiz.getQuestions().get(0).getAlternatives())
                .allSatisfy(alternative ->
                        assertThat(alternative.getQuestion()).isSameAs(savedQuiz.getQuestions().get(0)));
        verify(quizRepository).save(savedQuiz);
    }

    @Test
    void doesNotReturnQuizOwnedByAnotherUser() {
        UUID quizId = UUID.randomUUID();
        User requestingUser = new User();
        QuizRepository quizRepository = mock(QuizRepository.class);
        when(quizRepository.findByIdAndCreatorId(quizId, requestingUser.getId()))
                .thenReturn(Optional.empty());

        QuizService service = new QuizService(baseText -> new Quiz(), quizRepository);

        assertThatThrownBy(() -> service.findByIdAndCreator(quizId, requestingUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(quizRepository).findByIdAndCreatorId(quizId, requestingUser.getId());
    }

    @Test
    void replacesQuestionsAndAlternativesForOwnedQuiz() {
        UUID quizId = UUID.randomUUID();
        User creator = new User();
        Quiz quiz = new Quiz(
                "Quiz",
                List.of(new Question(
                        "Pergunta antiga",
                        List.of(
                                new Alternative("Correta", true),
                                new Alternative("Errada", false)
                        )
                ))
        );
        QuizRepository quizRepository = mock(QuizRepository.class);
        when(quizRepository.findByIdAndCreatorId(quizId, creator.getId())).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);
        QuizService service = new QuizService(baseText -> new Quiz(), quizRepository);

        Quiz updated = service.updateQuestions(
                quizId,
                List.of(new Question(
                        "  Pergunta nova  ",
                        List.of(
                                new Alternative("  Nova correta  ", true),
                                new Alternative("Nova errada", false)
                        )
                )),
                creator
        );

        assertThat(updated.getQuestions()).hasSize(1);
        assertThat(updated.getQuestions().get(0).getStatement()).isEqualTo("Pergunta nova");
        assertThat(updated.getQuestions().get(0).getAlternatives())
                .extracting(Alternative::getText)
                .containsExactly("Nova correta", "Nova errada");
        assertThat(updated.getQuestions().get(0).getQuiz()).isSameAs(updated);
        verify(quizRepository).save(quiz);
    }

    @Test
    void rejectsQuestionWithoutExactlyOneCorrectAlternative() {
        QuizService service = new QuizService(baseText -> new Quiz(), mock(QuizRepository.class));

        assertThatThrownBy(() -> service.updateQuestions(
                UUID.randomUUID(),
                List.of(new Question(
                        "Pergunta",
                        List.of(
                                new Alternative("A", false),
                                new Alternative("B", false)
                        )
                )),
                new User()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
