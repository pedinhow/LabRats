package br.com.starter.domain.quizattempt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.com.starter.domain.auth.Auth;
import br.com.starter.domain.profile.Profile;
import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizRepository;
import br.com.starter.domain.role.Role;
import br.com.starter.domain.user.User;
import br.com.starter.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class QuizAttemptRepositoryTest {
    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAttemptAndAnswerSnapshots() {
        User participant = saveUser();
        Quiz quiz = new Quiz(
                "Quiz persistido",
                List.of(new Question(
                        "Pergunta",
                        List.of(
                                new Alternative("Correta", true),
                                new Alternative("Errada", false)
                        )
                ))
        );
        quiz.setCreator(participant);
        quiz = quizRepository.saveAndFlush(quiz);

        Question question = quiz.getQuestions().get(0);
        Alternative correct = question.getAlternatives().get(0);
        QuizAttempt attempt = new QuizAttempt(quiz, participant, 1);
        attempt.complete(List.of(new UserAnswer(
                question.getId(),
                question.getStatement(),
                correct.getId(),
                correct.getText(),
                correct.getId(),
                correct.getText(),
                true
        )), 1);

        QuizAttempt saved = attemptRepository.saveAndFlush(attempt);
        entityManager.clear();

        QuizAttempt reloaded = attemptRepository.findByIdAndParticipantId(saved.getId(), participant.getId())
                .orElseThrow();
        assertThat(reloaded.getScore()).isEqualTo(1);
        assertThat(reloaded.getAnswers()).hasSize(1);
        assertThat(reloaded.getAnswers().get(0).getQuestionStatement()).isEqualTo("Pergunta");
        assertThat(reloaded.getAnswers().get(0).isCorrect()).isTrue();
    }

    private User saveUser() {
        Auth auth = new Auth();
        auth.setUsername("attempt-user");
        auth.setPassword("encoded-password");

        Profile profile = new Profile();
        profile.setName("Attempt User");

        Role role = new Role();
        role.setName("ROLE_ATTEMPT_USER");

        User user = new User();
        user.setAuth(auth);
        user.setProfile(profile);
        user.setRole(role);
        user.setPrivileges(List.of());
        return userRepository.saveAndFlush(user);
    }
}
