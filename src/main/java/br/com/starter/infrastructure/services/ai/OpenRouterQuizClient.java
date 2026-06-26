package br.com.starter.infrastructure.services.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import br.com.starter.infrastructure.services.ai.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.starter.domain.quiz.Alternative;
import br.com.starter.domain.quiz.Question;
import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.quiz.QuizGeneratorClient;

import java.util.Base64;

@Service
public class OpenRouterQuizClient implements QuizGeneratorClient {
    private static final String MODEL = "openrouter/owl-alpha";
    private static final String SYSTEM_PROMPT = """
            Voce e um gerador de quizzes automatico. Analise o texto fornecido e retorne exatamente 3 perguntas de multipla escolha com 4 alternativas cada.
            Retorne somente JSON valido, sem markdown, sem explicacoes e sem texto antes ou depois.
            Use exatamente este formato:
            {
              "title": "Titulo descritivo sobre o tema do conteudo (maximo 4 palavras, sem aspas, sem prefixo Quiz)",
              "questions": [
                {
                  "statement": "Pergunta",
                  "alternatives": [
                    { "text": "Alternativa A", "isCorrect": false },
                    { "text": "Alternativa B", "isCorrect": true },
                    { "text": "Alternativa C", "isCorrect": false },
                    { "text": "Alternativa D", "isCorrect": false }
                  ]
                }
              ]
            }
            """;

    private final String apiUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterQuizClient(
            @Value("${ai.openrouter.api-url}") String apiUrl,
            @Value("${ai.openrouter.api-key}") String apiKey,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper
    ) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Quiz generateFromText(String baseText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave da OpenRouter nao configurada.");
        }

        OpenRouterRequestDTO request = new OpenRouterRequestDTO(
                MODEL,
                List.of(
                        new OpenRouterMessageDTO("system", SYSTEM_PROMPT),
                        new OpenRouterMessageDTO("user", baseText)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<OpenRouterResponseDTO> response = restTemplate.postForEntity(
                    apiUrl,
                    new HttpEntity<>(request, headers),
                    OpenRouterResponseDTO.class
            );

            String generatedContent = extractGeneratedContent(response.getBody());
            return parseQuiz(generatedContent);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao chamar a OpenRouter.", exception);
        }
    }


    @Override
    public Quiz generateFromPdf(String filename, byte[] pdfBytes, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave da OpenRouter nao configurada.");
        }

        OpenRouterRequestDTO request = new OpenRouterRequestDTO(
                MODEL,
                List.of(
                        new OpenRouterMessageDTO("system", SYSTEM_PROMPT),
                        new OpenRouterMultimodalMessageDTO("user", List.of(
                                OpenRouterContentPartDTO.pdf(filename, pdfBytes),
                                OpenRouterContentPartDTO.text(userMessage)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<OpenRouterResponseDTO> response = restTemplate.postForEntity(
                    apiUrl,
                    new HttpEntity<>(request, headers),
                    OpenRouterResponseDTO.class
            );

            String generatedContent = extractGeneratedContent(response.getBody());
            return parseQuiz(generatedContent);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao chamar a OpenRouter.", exception);
        }
    }

    private String extractGeneratedContent(OpenRouterResponseDTO response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta invalida da OpenRouter.");
        }

        OpenRouterMessageDTO message = response.choices().get(0).message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Conteudo vazio retornado pela OpenRouter.");
        }

        return message.content();
    }

    private Quiz parseQuiz(String generatedContent) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(generatedContent));
            Quiz quiz = toQuiz(root);
            validateQuiz(quiz);
            return quiz;
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A OpenRouter retornou um JSON de quiz invalido.", exception);
        }
    }

    private Quiz toQuiz(JsonNode root) {
        JsonNode questionNodes = root.isArray()
                ? root
                : firstExistingArray(root, "questions", "perguntas", "questoes");

        String title = root.isObject()
                ? firstText(root, "title", "theme", "tema", "titulo")
                : null;

        List<Question> questions = new ArrayList<>();
        if (questionNodes != null) {
            for (JsonNode questionNode : questionNodes) {
                Question question = toQuestion(questionNode);
                if (question.getStatement() != null && !question.getStatement().isBlank()) {
                    questions.add(question);
                }
            }
        }

        return new Quiz(title, questions);
    }

    private Question toQuestion(JsonNode questionNode) {
        String statement = firstText(questionNode, "statement", "question", "pergunta", "enunciado", "text", "texto");
        JsonNode alternativeNodes = firstExistingArray(questionNode, "alternatives", "options", "alternativas", "opcoes");

        List<Alternative> alternatives = new ArrayList<>();
        if (alternativeNodes != null) {
            int index = 0;
            for (JsonNode alternativeNode : alternativeNodes) {
                String text = alternativeNode.isTextual()
                        ? alternativeNode.asText()
                        : firstText(alternativeNode, "text", "texto", "option", "alternative", "alternativa", "content", "conteudo", "resposta");

                if (text != null && !text.isBlank()) {
                    alternatives.add(new Alternative(text, isCorrectAlternative(questionNode, alternativeNode, text, index)));
                }
                index++;
            }
        }

        return new Question(statement, alternatives);
    }

    private Boolean isCorrectAlternative(JsonNode questionNode, JsonNode alternativeNode, String alternativeText, int index) {
        Boolean directValue = firstBoolean(alternativeNode, "isCorrect", "correct", "correta", "is_correct");
        if (directValue != null) {
            return directValue;
        }

        JsonNode correctNode = firstExisting(questionNode, "correctAnswer", "correct_answer", "respostaCorreta", "alternativaCorreta", "correta", "correct");
        if (correctNode == null || correctNode.isNull()) {
            return false;
        }

        if (correctNode.isInt()) {
            int value = correctNode.asInt();
            return value == index || value == index + 1;
        }

        String correctValue = correctNode.asText("").trim();
        if (correctValue.length() == 1) {
            char letter = Character.toUpperCase(correctValue.charAt(0));
            if (letter >= 'A' && letter <= 'D') {
                return index == letter - 'A';
            }
        }

        return alternativeText.equalsIgnoreCase(correctValue);
    }

    private JsonNode firstExistingArray(JsonNode node, String... fieldNames) {
        JsonNode value = firstExisting(node, fieldNames);
        return value != null && value.isArray() ? value : null;
    }

    private JsonNode firstExisting(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                return value;
            }
        }

        return null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        JsonNode value = firstExisting(node, fieldNames);
        return value == null ? null : value.asText(null);
    }

    private Boolean firstBoolean(JsonNode node, String... fieldNames) {
        JsonNode value = firstExisting(node, fieldNames);
        return value != null && value.isBoolean() ? value.asBoolean() : null;
    }

    private void validateQuiz(Quiz quiz) {
        if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A OpenRouter nao retornou perguntas validas.");
        }

        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            quiz.setTitle("Quiz gerado");
        }
    }

    private String extractJson(String generatedContent) {
        String content = generatedContent.trim();
        int firstObjectBrace = content.indexOf('{');
        int firstArrayBrace = content.indexOf('[');

        if (firstArrayBrace >= 0 && (firstObjectBrace < 0 || firstArrayBrace < firstObjectBrace)) {
            int lastArrayBrace = content.lastIndexOf(']');
            if (lastArrayBrace > firstArrayBrace) {
                return content.substring(firstArrayBrace, lastArrayBrace + 1);
            }
        }

        int lastObjectBrace = content.lastIndexOf('}');
        if (firstObjectBrace < 0 || lastObjectBrace < firstObjectBrace) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A OpenRouter nao retornou JSON valido.");
        }

        return content.substring(firstObjectBrace, lastObjectBrace + 1);
    }
}
