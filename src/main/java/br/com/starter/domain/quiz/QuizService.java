package br.com.starter.domain.quiz;

import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.starter.domain.user.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {
    private final QuizGeneratorClient quizGeneratorClient;
    private final QuizRepository quizRepository;

    public QuizService(QuizGeneratorClient quizGeneratorClient, QuizRepository quizRepository) {
        this.quizGeneratorClient = quizGeneratorClient;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public Quiz generate(String baseText, User creator) {
        if (baseText == null || baseText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto base não pode ser vazio.");
        }
        if (creator == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        Quiz quiz = quizGeneratorClient.generateFromText(baseText.trim());
        quiz.setTitle(buildDefaultTitle(baseText));
        quiz.setCreator(creator);

        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz rename(UUID quizId, String title, User creator) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TÃ­tulo do quiz nÃ£o pode ser vazio.");
        }

        Quiz quiz = findByIdAndCreator(quizId, creator);
        quiz.setTitle(title.trim());

        return quizRepository.save(quiz);
    }

    @Transactional
    public List<Quiz> findAllByCreator(User creator) {
        List<Quiz> quizzes = quizRepository.findAllByCreatorIdOrderByCreatedAtDesc(creator.getId());
        quizzes.forEach(this::initializeQuestions);
        return quizzes;
    }

    @Transactional
    public Quiz findByIdAndCreator(UUID quizId, User creator) {
        Quiz quiz = quizRepository.findByIdAndCreatorId(quizId, creator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não encontrado."));
        initializeQuestions(quiz);
        return quiz;
    }

    private void initializeQuestions(Quiz quiz) {
        quiz.getQuestions().forEach(question -> question.getAlternatives().size());
    }

    private String buildDefaultTitle(String baseText) {
        String titleSuffix = Arrays.stream(baseText.trim().split("\\s+"))
                .limit(2)
                .collect(Collectors.joining(" "));

        return titleSuffix.isBlank() ? "Quiz" : "Quiz " + titleSuffix;
    }
}
