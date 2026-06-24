package br.com.starter.domain.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.starter.domain.auth.Auth;
import br.com.starter.domain.profile.Profile;
import br.com.starter.domain.role.Role;
import br.com.starter.domain.user.User;
import br.com.starter.domain.user.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
class QuizRepositoryTest {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesQuestionsAndAlternativesInCascade() {
        User creator = saveUser("creator-cascade");
        Quiz quiz = buildQuiz(creator, "Quiz persistido");

        Quiz savedQuiz = quizRepository.saveAndFlush(quiz);
        entityManager.clear();

        Quiz reloadedQuiz = quizRepository.findById(savedQuiz.getId()).orElseThrow();

        assertThat(reloadedQuiz.getCreator().getId()).isEqualTo(creator.getId());
        assertThat(reloadedQuiz.getQuestions()).hasSize(1);
        assertThat(reloadedQuiz.getQuestions().get(0).getDurationSeconds()).isEqualTo(15);
        assertThat(reloadedQuiz.getQuestions().get(0).getAlternatives())
                .extracting(Alternative::getText)
                .containsExactly("Alternativa correta", "Alternativa incorreta");
    }

    @Test
    void onlyFindsQuizWhenItBelongsToRequestedCreator() {
        User owner = saveUser("quiz-owner");
        User otherUser = saveUser("other-user");
        Quiz savedQuiz = quizRepository.saveAndFlush(buildQuiz(owner, "Quiz privado"));

        assertThat(quizRepository.findByIdAndCreatorId(savedQuiz.getId(), owner.getId())).isPresent();
        assertThat(quizRepository.findByIdAndCreatorId(savedQuiz.getId(), otherUser.getId())).isEmpty();
        assertThat(quizRepository.findAllByCreatorIdOrderByCreatedAtDesc(otherUser.getId())).isEmpty();
    }

    @Test
    void replacesQuestionsAndRemovesOldChildren() {
        User creator = saveUser("creator-edit");
        Quiz quiz = quizRepository.saveAndFlush(buildQuiz(creator, "Quiz editavel"));
        UUID oldQuestionId = quiz.getQuestions().get(0).getId();
        UUID oldAlternativeId = quiz.getQuestions().get(0).getAlternatives().get(0).getId();

        quiz.setQuestions(List.of(new Question(
                "Pergunta atualizada",
                List.of(
                        new Alternative("Nova correta", true),
                        new Alternative("Nova errada", false)
                )
        )));
        quizRepository.saveAndFlush(quiz);
        entityManager.clear();

        Quiz reloaded = quizRepository.findById(quiz.getId()).orElseThrow();
        assertThat(reloaded.getQuestions()).extracting(Question::getStatement)
                .containsExactly("Pergunta atualizada");
        assertThat(entityManager.find(Question.class, oldQuestionId)).isNull();
        assertThat(entityManager.find(Alternative.class, oldAlternativeId)).isNull();
    }

    private User saveUser(String username) {
        Auth auth = new Auth();
        auth.setUsername(username);
        auth.setPassword("encoded-password");

        Profile profile = new Profile();
        profile.setName(username);

        Role role = new Role();
        role.setName("ROLE_" + username.toUpperCase().replace('-', '_'));

        User user = new User();
        user.setAuth(auth);
        user.setProfile(profile);
        user.setRole(role);
        user.setPrivileges(List.of());

        return userRepository.saveAndFlush(user);
    }

    private Quiz buildQuiz(User creator, String title) {
        Quiz quiz = new Quiz(
                title,
                List.of(new Question(
                        "Qual alternativa esta correta?",
                        List.of(
                                new Alternative("Alternativa correta", true),
                                new Alternative("Alternativa incorreta", false)
                        )
                ))
        );
        quiz.setCreator(creator);
        return quiz;
    }
}
