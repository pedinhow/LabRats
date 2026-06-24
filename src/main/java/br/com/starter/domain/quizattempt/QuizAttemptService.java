package br.com.starter.domain.quizattempt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizService;
import br.com.starter.domain.user.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizAttemptService {
    private final QuizAttemptRepository attemptRepository;
    private final QuizService quizService;

    public QuizAttemptService(QuizAttemptRepository attemptRepository, QuizService quizService) {
        this.attemptRepository = attemptRepository;
        this.quizService = quizService;
    }

    @Transactional
    public QuizAttempt start(UUID quizId, User participant) {
        Quiz quiz = quizService.findByIdAndCreator(quizId, participant);
        if (quiz.getQuestions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quiz nao possui perguntas.");
        }

        return attemptRepository.save(new QuizAttempt(quiz, participant, quiz.getQuestions().size()));
    }

    @Transactional
    public QuizAttempt submit(
            UUID attemptId,
            List<SubmittedAnswer> submittedAnswers,
            User participant
    ) {
        QuizAttempt attempt = attemptRepository.findByIdAndParticipantId(attemptId, participant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));

        if (attempt.isCompleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta tentativa ja foi finalizada.");
        }

        Quiz quiz = quizService.findByIdAndCreator(attempt.getQuiz().getId(), participant);
        Map<UUID, UUID> answerByQuestion = validateAndIndexAnswers(quiz, submittedAnswers);
        List<UserAnswer> answers = new ArrayList<>();
        int score = 0;

        for (Question question : quiz.getQuestions()) {
            UUID selectedAlternativeId = answerByQuestion.get(question.getId());
            Alternative selected = selectedAlternativeId == null
                    ? null
                    : findAlternative(question, selectedAlternativeId);
            Alternative correct = question.getAlternatives().stream()
                    .filter(alternative -> Boolean.TRUE.equals(alternative.getIsCorrect()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Uma pergunta do quiz nao possui alternativa correta."
                    ));
            boolean isCorrect = selected != null && selected.getId().equals(correct.getId());
            if (isCorrect) {
                score++;
            }

            answers.add(new UserAnswer(
                    question.getId(),
                    question.getStatement(),
                    selected == null ? null : selected.getId(),
                    selected == null ? null : selected.getText(),
                    correct.getId(),
                    correct.getText(),
                    isCorrect
            ));
        }

        attempt.complete(answers, score);
        QuizAttempt savedAttempt = attemptRepository.save(attempt);
        savedAttempt.getQuiz().getTitle();
        savedAttempt.getAnswers().size();
        return savedAttempt;
    }

    private Map<UUID, UUID> validateAndIndexAnswers(
            Quiz quiz,
            List<SubmittedAnswer> submittedAnswers
    ) {
        if (submittedAnswers == null || submittedAnswers.size() != quiz.getQuestions().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas as perguntas devem ser respondidas.");
        }

        Map<UUID, UUID> answerByQuestion = new HashMap<>();
        for (SubmittedAnswer answer : submittedAnswers) {
            if (answer == null || answer.questionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta invalida.");
            }
            if (answerByQuestion.put(answer.questionId(), answer.alternativeId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma pergunta foi respondida mais de uma vez.");
            }
        }

        boolean answersEveryQuestion = quiz.getQuestions().stream()
                .allMatch(question -> answerByQuestion.containsKey(question.getId()));
        if (!answersEveryQuestion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas as perguntas devem ser respondidas.");
        }

        return answerByQuestion;
    }

    private Alternative findAlternative(Question question, UUID alternativeId) {
        return question.getAlternatives().stream()
                .filter(alternative -> alternative.getId().equals(alternativeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A alternativa selecionada nao pertence a pergunta."
                ));
    }
}
