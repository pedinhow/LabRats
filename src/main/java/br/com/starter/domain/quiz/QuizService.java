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
    public Quiz updateQuestions(UUID quizId, List<Question> questions, User creator) {
        validateQuestions(questions);

        Quiz quiz = findByIdAndCreator(quizId, creator);
        quiz.setQuestions(questions);
        return quizRepository.save(quiz);
    }

    @Transactional
    public void delete(UUID quizId, User creator) {
        Quiz quiz = findByIdAndCreator(quizId, creator);
        quizRepository.delete(quiz);
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

    @Transactional
    public Quiz saveGeneratedQuiz(Quiz quiz, User creator) {
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Quiz gerado invalido.");
        }
        if (creator == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao autenticado.");
        }

        quiz.setCreator(creator);
        return quizRepository.save(quiz);
    }

    private void initializeQuestions(Quiz quiz) {
        quiz.getQuestions().forEach(question -> question.getAlternatives().size());
    }

    private void validateQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quiz deve ter pelo menos uma pergunta.");
        }

        for (Question question : questions) {
            if (question == null || question.getStatement() == null || question.getStatement().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O enunciado nao pode ser vazio.");
            }
            question.setStatement(question.getStatement().trim());

            if (question.getDurationSeconds() == null || question.getDurationSeconds() < 5) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A duracao de cada pergunta deve ser de pelo menos cinco segundos."
                );
            }

            List<Alternative> alternatives = question.getAlternatives();
            if (alternatives == null || alternatives.size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cada pergunta deve ter pelo menos duas alternativas."
                );
            }

            int correctCount = 0;
            for (Alternative alternative : alternatives) {
                if (alternative == null || alternative.getText() == null || alternative.getText().trim().isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "O texto da alternativa nao pode ser vazio."
                    );
                }
                alternative.setText(alternative.getText().trim());
                if (Boolean.TRUE.equals(alternative.getIsCorrect())) {
                    correctCount++;
                }
            }

            if (correctCount != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cada pergunta deve ter exatamente uma alternativa correta."
                );
            }
        }
    }

    private String buildDefaultTitle(String baseText) {
        String titleSuffix = Arrays.stream(baseText.trim().split("\\s+"))
                .limit(2)
                .collect(Collectors.joining(" "));

        return titleSuffix.isBlank() ? "Quiz" : "Quiz " + titleSuffix;
    }

}
