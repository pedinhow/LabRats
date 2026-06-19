package br.com.starter.application.api.quiz;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.starter.application.api.common.ResponseDTO;
import br.com.starter.application.api.quiz.dto.GenerateQuizRequestDTO;
import br.com.starter.application.api.quiz.dto.QuizResponseDTO;
import br.com.starter.domain.quiz.QuizService;

@RestController
@RequestMapping("/quizzes")
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateQuizRequestDTO request) {
        return ResponseEntity.ok(
                new ResponseDTO<>(
                        QuizResponseDTO.fromQuiz(quizService.generate(request == null ? null : request.baseText()))
                )
        );
    }
}
