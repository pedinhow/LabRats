package br.com.starter.domain.quizattempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizService;
import br.com.starter.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class QuizAttemptServiceTest {

    @Test
    void startsAttemptForOwnedQuiz() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizService quizService = mock(QuizService.class);
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(quizService.findByIdAndCreator(quiz.getId(), participant)).thenReturn(quiz);
        when(repository.save(org.mockito.ArgumentMatchers.any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizAttempt attempt = new QuizAttemptService(repository, quizService).start(quiz.getId(), participant);

        assertThat(attempt.getQuiz()).isSameAs(quiz);
        assertThat(attempt.getParticipant()).isSameAs(participant);
        assertThat(attempt.getTotalQuestions()).isEqualTo(2);
        verify(repository).save(attempt);
    }

    @Test
    void submitsAllAnswersAndCalculatesScore() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizAttempt attempt = new QuizAttempt(quiz, participant, quiz.getQuestions().size());
        UUID attemptId = UUID.randomUUID();
        QuizService quizService = mock(QuizService.class);
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(repository.findByIdAndParticipantId(attemptId, participant.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizService.findByIdAndCreator(quiz.getId(), participant)).thenReturn(quiz);
        when(repository.save(attempt)).thenReturn(attempt);

        Question firstQuestion = quiz.getQuestions().get(0);
        Question secondQuestion = quiz.getQuestions().get(1);
        List<SubmittedAnswer> answers = List.of(
                new SubmittedAnswer(firstQuestion.getId(), firstQuestion.getAlternatives().get(0).getId()),
                new SubmittedAnswer(secondQuestion.getId(), secondQuestion.getAlternatives().get(1).getId())
        );

        QuizAttempt result = new QuizAttemptService(repository, quizService)
                .submit(attemptId, answers, participant);

        assertThat(result.getScore()).isEqualTo(1);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getAnswers()).hasSize(2);
        assertThat(result.getAnswers()).extracting(UserAnswer::isCorrect).containsExactly(true, false);
        assertThat(result.getAnswers().get(0).getCorrectAlternativeText()).isEqualTo("Correta 1");
        verify(repository).save(attempt);
    }

    @Test
    void rejectsIncompleteSubmission() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizAttempt attempt = new QuizAttempt(quiz, participant, quiz.getQuestions().size());
        UUID attemptId = UUID.randomUUID();
        QuizService quizService = mock(QuizService.class);
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(repository.findByIdAndParticipantId(attemptId, participant.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizService.findByIdAndCreator(quiz.getId(), participant)).thenReturn(quiz);

        List<SubmittedAnswer> incompleteAnswers = List.of(new SubmittedAnswer(
                quiz.getQuestions().get(0).getId(),
                quiz.getQuestions().get(0).getAlternatives().get(0).getId()
        ));

        assertThatThrownBy(() -> new QuizAttemptService(repository, quizService)
                .submit(attemptId, incompleteAnswers, participant))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repository, never()).save(attempt);
    }

    @Test
    void rejectsAlternativeFromAnotherQuestion() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizAttempt attempt = new QuizAttempt(quiz, participant, quiz.getQuestions().size());
        UUID attemptId = UUID.randomUUID();
        QuizService quizService = mock(QuizService.class);
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(repository.findByIdAndParticipantId(attemptId, participant.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizService.findByIdAndCreator(quiz.getId(), participant)).thenReturn(quiz);

        List<SubmittedAnswer> answers = List.of(
                new SubmittedAnswer(
                        quiz.getQuestions().get(0).getId(),
                        quiz.getQuestions().get(1).getAlternatives().get(0).getId()
                ),
                new SubmittedAnswer(
                        quiz.getQuestions().get(1).getId(),
                        quiz.getQuestions().get(1).getAlternatives().get(0).getId()
                )
        );

        assertThatThrownBy(() -> new QuizAttemptService(repository, quizService)
                .submit(attemptId, answers, participant))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAlreadyCompletedAttempt() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizAttempt attempt = new QuizAttempt(quiz, participant, quiz.getQuestions().size());
        attempt.complete(List.of(), 0);
        UUID attemptId = UUID.randomUUID();
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(repository.findByIdAndParticipantId(attemptId, participant.getId()))
                .thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> new QuizAttemptService(repository, mock(QuizService.class))
                .submit(attemptId, List.of(), participant))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void countsExpiredQuestionAsWrongAnswer() {
        User participant = new User();
        Quiz quiz = buildQuiz();
        QuizAttempt attempt = new QuizAttempt(quiz, participant, quiz.getQuestions().size());
        UUID attemptId = UUID.randomUUID();
        QuizService quizService = mock(QuizService.class);
        QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
        when(repository.findByIdAndParticipantId(attemptId, participant.getId()))
                .thenReturn(Optional.of(attempt));
        when(quizService.findByIdAndCreator(quiz.getId(), participant)).thenReturn(quiz);
        when(repository.save(attempt)).thenReturn(attempt);

        Question firstQuestion = quiz.getQuestions().get(0);
        Question secondQuestion = quiz.getQuestions().get(1);
        QuizAttempt result = new QuizAttemptService(repository, quizService).submit(
                attemptId,
                List.of(
                        new SubmittedAnswer(firstQuestion.getId(), null),
                        new SubmittedAnswer(secondQuestion.getId(), secondQuestion.getAlternatives().get(0).getId())
                ),
                participant
        );

        assertThat(result.getScore()).isEqualTo(1);
        assertThat(result.getAnswers().get(0).getSelectedAlternativeId()).isNull();
        assertThat(result.getAnswers().get(0).isCorrect()).isFalse();
    }

    private Quiz buildQuiz() {
        Quiz quiz = new Quiz(
                "Quiz",
                List.of(
                        new Question(
                                "Pergunta 1",
                                List.of(
                                        new Alternative("Correta 1", true),
                                        new Alternative("Errada 1", false)
                                )
                        ),
                        new Question(
                                "Pergunta 2",
                                List.of(
                                        new Alternative("Correta 2", true),
                                        new Alternative("Errada 2", false)
                                )
                        )
                )
        );
        quiz.setId(UUID.randomUUID());
        quiz.getQuestions().forEach(question -> {
            question.setId(UUID.randomUUID());
            question.getAlternatives().forEach(alternative -> alternative.setId(UUID.randomUUID()));
        });
        return quiz;
    }
}
