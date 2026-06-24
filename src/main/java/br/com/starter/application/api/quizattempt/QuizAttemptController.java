package br.com.starter.application.api.quizattempt;

import java.util.UUID;

import br.com.starter.application.api.common.ResponseDTO;
import br.com.starter.application.api.quizattempt.dto.QuizAttemptResultResponseDTO;
import br.com.starter.application.api.quizattempt.dto.SubmitQuizAttemptRequestDTO;
import br.com.starter.domain.quizattempt.QuizAttempt;
import br.com.starter.domain.quizattempt.QuizAttemptService;
import br.com.starter.domain.quizattempt.SubmittedAnswer;
import br.com.starter.domain.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz-attempts")
public class QuizAttemptController {
    private final QuizAttemptService attemptService;

    public QuizAttemptController(QuizAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ResponseDTO<QuizAttemptResultResponseDTO>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID attemptId,
            @RequestBody SubmitQuizAttemptRequestDTO request
    ) {
        QuizAttempt attempt = attemptService.submit(
                attemptId,
                request == null || request.answers() == null
                        ? null
                        : request.answers().stream()
                                .map(answer -> answer == null
                                        ? null
                                        : new SubmittedAnswer(answer.questionId(), answer.alternativeId()))
                                .toList(),
                userDetails.getUser()
        );

        return ResponseEntity.ok(new ResponseDTO<>(QuizAttemptResultResponseDTO.fromAttempt(attempt)));
    }
}
