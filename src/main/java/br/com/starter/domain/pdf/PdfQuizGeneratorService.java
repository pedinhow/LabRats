package br.com.starter.domain.pdf;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizGeneratorClient;

@Service
public class PdfQuizGeneratorService {
    public static final long MAX_PDF_SIZE_BYTES = 20L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = { 0x25, 0x50, 0x44, 0x46 };

    private final QuizGeneratorClient quizGeneratorClient;

    public PdfQuizGeneratorService(QuizGeneratorClient quizGeneratorClient) {
        this.quizGeneratorClient = quizGeneratorClient;
    }

    public Quiz generateQuiz(MultipartFile file, String prompt) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo PDF nao informado.");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo PDF vazio.");
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo PDF excede o limite de 20 MB.");
        }

        byte[] bytes = readBytes(file);

        if (!isPdf(bytes)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O arquivo enviado nao e um PDF valido.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento.pdf";
        String userMessage = (prompt != null && !prompt.isBlank())
                ? prompt.trim()
                : "Gere um quiz baseado neste documento.";

        return quizGeneratorClient.generateFromPdf(filename, bytes, userMessage);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo PDF.", exception);
        }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}