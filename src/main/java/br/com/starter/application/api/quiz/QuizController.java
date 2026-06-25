package br.com.starter.application.api.quiz;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.starter.application.api.common.ResponseDTO;
import br.com.starter.application.api.quiz.dto.GenerateQuizRequestDTO;
import br.com.starter.application.api.quiz.dto.GenerateQuizResponseDTO;
import br.com.starter.application.api.quiz.dto.QuizEditResponseDTO;
import br.com.starter.application.api.quiz.dto.QuizResponseDTO;
import br.com.starter.application.api.quiz.dto.UpdateQuizQuestionsRequestDTO;
import br.com.starter.application.api.quiz.dto.UpdateQuizTitleRequestDTO;
import br.com.starter.application.api.quizattempt.dto.StartQuizAttemptResponseDTO;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizService;
import br.com.starter.domain.quizattempt.QuizAttempt;
import br.com.starter.domain.quizattempt.QuizAttemptService;
import br.com.starter.domain.user.CustomUserDetails;

@RestController
@RequestMapping("/quizzes")
public class QuizController {
    private final QuizService quizService;
    private final QuizAttemptService attemptService;

    public QuizController(QuizService quizService, QuizAttemptService attemptService) {
        this.quizService = quizService;
        this.attemptService = attemptService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ResponseDTO<GenerateQuizResponseDTO>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GenerateQuizRequestDTO request
    ) {
        Quiz quiz = quizService.generate(
                request == null ? null : request.baseText(),
                userDetails.getUser()
        );

        return ResponseEntity.ok(new ResponseDTO<>(new GenerateQuizResponseDTO(quiz.getId())));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<List<QuizResponseDTO>>> findMine(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<QuizResponseDTO> quizzes = quizService.findAllByCreator(userDetails.getUser()).stream()
                .map(QuizResponseDTO::fromQuiz)
                .toList();

        return ResponseEntity.ok(new ResponseDTO<>(quizzes));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseDTO<QuizResponseDTO>> findById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId
    ) {
        Quiz quiz = quizService.findByIdAndCreator(quizId, userDetails.getUser());
        return ResponseEntity.ok(new ResponseDTO<>(QuizResponseDTO.fromQuiz(quiz)));
    }

    @GetMapping("/{quizId}/edit")
    public ResponseEntity<ResponseDTO<QuizEditResponseDTO>> findForEditing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId
    ) {
        Quiz quiz = quizService.findByIdAndCreator(quizId, userDetails.getUser());
        return ResponseEntity.ok(new ResponseDTO<>(QuizEditResponseDTO.fromQuiz(quiz)));
    }

    @PutMapping("/{quizId}/questions")
    public ResponseEntity<ResponseDTO<QuizEditResponseDTO>> updateQuestions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId,
            @RequestBody UpdateQuizQuestionsRequestDTO request
    ) {
        Quiz quiz = quizService.updateQuestions(
                quizId,
                request == null ? null : request.toQuestions(),
                userDetails.getUser()
        );
        return ResponseEntity.ok(new ResponseDTO<>(QuizEditResponseDTO.fromQuiz(quiz)));
    }

    @PostMapping("/{quizId}/attempts")
    public ResponseEntity<ResponseDTO<StartQuizAttemptResponseDTO>> startAttempt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId
    ) {
        QuizAttempt attempt = attemptService.start(quizId, userDetails.getUser());
        return ResponseEntity.ok(new ResponseDTO<>(StartQuizAttemptResponseDTO.fromAttempt(attempt)));
    }

    @PatchMapping("/{quizId}/title")
    public ResponseEntity<ResponseDTO<QuizResponseDTO>> rename(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId,
            @RequestBody UpdateQuizTitleRequestDTO request
    ) {
        Quiz quiz = quizService.rename(
                quizId,
                request == null ? null : request.title(),
                userDetails.getUser()
        );

        return ResponseEntity.ok(new ResponseDTO<>(QuizResponseDTO.fromQuiz(quiz)));
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<ResponseDTO<Boolean>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID quizId
    ) {
        quizService.delete(quizId, userDetails.getUser());
        return ResponseEntity.ok(new ResponseDTO<>(true));
    }
}
