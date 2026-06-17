package br.com.starter.domain.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {
    private final QuizGeneratorClient quizGeneratorClient;

    public QuizService(QuizGeneratorClient quizGeneratorClient) {
        this.quizGeneratorClient = quizGeneratorClient;
    }

    public Quiz generate(String baseText) {
        if (baseText == null || baseText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto base não pode ser vazio.");
        }

        return quizGeneratorClient.generateFromText(baseText.trim());
    }
}
