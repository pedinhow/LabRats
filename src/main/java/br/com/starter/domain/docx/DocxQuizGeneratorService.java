package br.com.starter.domain.docx;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizGeneratorClient;

@Service
public class DocxQuizGeneratorService {
    public static final long MAX_DOCX_SIZE_BYTES = 20L * 1024 * 1024;
    private static final byte[] DOCX_MAGIC = { 0x50, 0x4B, 0x03, 0x04 };

    private final QuizGeneratorClient quizGeneratorClient;

    public DocxQuizGeneratorService(QuizGeneratorClient quizGeneratorClient) {
        this.quizGeneratorClient = quizGeneratorClient;
    }

    public Quiz generateQuiz(MultipartFile file, String prompt) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo DOCX nao informado.");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo DOCX vazio.");
        }
        if (file.getSize() > MAX_DOCX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo DOCX excede o limite de 20 MB.");
        }

        byte[] bytes = readBytes(file);

        if (!isDocx(bytes)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O arquivo enviado nao e um DOCX valido.");
        }

        String extractedText = extractText(bytes);

        String baseText = (prompt != null && !prompt.isBlank())
                ? prompt.trim() + "\n\n" + extractedText
                : extractedText;

        return quizGeneratorClient.generateFromText(baseText);
    }

    private String extractText(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.isBlank()) {
                    text.append(paragraphText).append("\n");
                }
            }
            String result = text.toString().trim();
            if (result.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nao foi possivel extrair texto do documento.");
            }
            return result;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Erro ao processar o documento DOCX.", exception);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo DOCX.", exception);
        }
    }

    private boolean isDocx(byte[] bytes) {
        if (bytes.length < DOCX_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < DOCX_MAGIC.length; i++) {
            if (bytes[i] != DOCX_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}