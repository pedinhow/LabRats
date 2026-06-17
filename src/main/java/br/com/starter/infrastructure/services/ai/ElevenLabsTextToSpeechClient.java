package br.com.starter.infrastructure.services.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.starter.domain.audio.TextToSpeechGateway;
import br.com.starter.infrastructure.services.ai.dto.ElevenLabsTextToSpeechRequestDTO;

@Service
public class ElevenLabsTextToSpeechClient implements TextToSpeechGateway {
    private static final String API_BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech";
    private static final String API_KEY_HEADER = "xi-api-key";
    private static final String API_KEY_ENV = "ELEVENLABS_API_KEY";
    private static final String API_KEY_PROPERTY = "elevenlabs.api-key";
    private static final String MODEL = "eleven_multilingual_v2";
    private static final String OUTPUT_FORMAT = "mp3_44100_128";
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");
    private static final int MAX_EXTERNAL_ERROR_LENGTH = 300;
    private static final String VOICE_ID_ENV = "ELEVENLABS_VOICE_ID";
    private static final String VOICE_ID_PROPERTY = "elevenlabs.voice-id";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final String voiceId;
    private final RestTemplate restTemplate;

    public ElevenLabsTextToSpeechClient(
            Environment environment,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.apiKey = readConfiguration(environment, API_KEY_PROPERTY, API_KEY_ENV);
        this.objectMapper = objectMapper;
        this.voiceId = readConfiguration(environment, VOICE_ID_PROPERTY, VOICE_ID_ENV);
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public byte[] synthesize(String text) {
        validateConfiguration();

        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    buildUrl(),
                    new HttpEntity<>(new ElevenLabsTextToSpeechRequestDTO(text, MODEL), buildHeaders()),
                    byte[].class
            );

            byte[] audio = response.getBody();
            if (audio == null || audio.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta vazia da ElevenLabs.");
            }

            return audio;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    buildHttpErrorMessage(exception),
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ElevenLabs indisponivel ou tempo limite excedido.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao chamar a ElevenLabs.", exception);
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave da ElevenLabs nao configurada.");
        }

        if (voiceId == null || voiceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Voice ID da ElevenLabs nao configurado.");
        }
    }

    private String buildUrl() {
        return UriComponentsBuilder.fromUriString(API_BASE_URL)
                .pathSegment(voiceId)
                .queryParam("output_format", OUTPUT_FORMAT)
                .build()
                .toUriString();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(AUDIO_MPEG));
        return headers;
    }

    private String readConfiguration(Environment environment, String propertyName, String environmentName) {
        String value = firstText(
                environment.getProperty(propertyName),
                environment.getProperty(environmentName),
                findLenientEnvironmentValue(environment, environmentName)
        );

        return value == null ? "" : value.trim();
    }

    private String findLenientEnvironmentValue(Environment environment, String environmentName) {
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return null;
        }

        for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName != null && propertyName.trim().equals(environmentName)) {
                        Object value = propertySource.getProperty(propertyName);
                        return value == null ? null : value.toString();
                    }
                }
            }
        }

        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }

    private String buildHttpErrorMessage(HttpStatusCodeException exception) {
        String message = "Erro HTTP ao chamar a ElevenLabs: " + exception.getStatusCode().value() + ".";
        String externalMessage = extractExternalErrorMessage(exception.getResponseBodyAsString());

        return externalMessage == null
                ? message
                : message + " Detalhe: " + externalMessage;
    }

    private String extractExternalErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return truncate(sanitize(firstExternalMessage(root)));
        } catch (JsonProcessingException exception) {
            return truncate(sanitize(responseBody));
        }
    }

    private String firstExternalMessage(JsonNode root) {
        Set<String> uniqueParts = new LinkedHashSet<>();
        List<String> parts = new ArrayList<>();
        collectText(root, parts, "status");
        collectText(root, parts, "code");
        collectText(root, parts, "message");
        collectText(root, parts, "detail");
        collectText(root, parts, "error");
        uniqueParts.addAll(parts);

        if (uniqueParts.isEmpty()) {
            return root == null ? null : root.toString();
        }

        return String.join(" - ", uniqueParts);
    }

    private void collectText(JsonNode node, List<String> parts, String fieldName) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isValueNode()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    parts.add(text);
                }
            }

            node.fields().forEachRemaining(entry -> collectText(entry.getValue(), parts, fieldName));
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, parts, fieldName));
        } else if ("detail".equals(fieldName) && node.isValueNode()) {
            String text = node.asText();
            if (StringUtils.hasText(text)) {
                parts.add(text);
            }
        }
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value
                .replaceAll("(?i)xi-api-key\\s*[:=]\\s*[^\\s,;}\"]+", "xi-api-key: [REDACTED]")
                .replaceAll("(?i)(api[_ -]?key|token|authorization)\\s*[:=]\\s*[^\\s,;}\"]+", "$1: [REDACTED]")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer [REDACTED]")
                .replaceAll("sk_[A-Za-z0-9_\\-]+", "[REDACTED]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_EXTERNAL_ERROR_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_EXTERNAL_ERROR_LENGTH) + "...";
    }
}
