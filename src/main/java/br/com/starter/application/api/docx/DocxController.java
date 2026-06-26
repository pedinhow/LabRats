package br.com.starter.application.api.docx;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.starter.application.api.common.ResponseDTO;
import br.com.starter.application.api.quiz.dto.GenerateQuizResponseDTO;
import br.com.starter.domain.docx.DocxQuizGeneratorService;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizService;
import br.com.starter.domain.user.CustomUserDetails;

@RestController
@RequestMapping("/docx")
public class DocxController {
    private final DocxQuizGeneratorService docxQuizGeneratorService;
    private final QuizService quizService;

    public DocxController(DocxQuizGeneratorService docxQuizGeneratorService, QuizService quizService) {
        this.docxQuizGeneratorService = docxQuizGeneratorService;
        this.quizService = quizService;
    }

    @PostMapping(
            value = "/generate-quiz",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDTO<GenerateQuizResponseDTO>> generateQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestPart(name = "prompt", required = false) String prompt
    ) {
        Quiz quiz = docxQuizGeneratorService.generateQuiz(file, prompt);
        Quiz saved = quizService.saveGeneratedQuiz(quiz, userDetails.getUser());
        return ResponseEntity.ok(new ResponseDTO<>(new GenerateQuizResponseDTO(saved.getId())));
    }
}