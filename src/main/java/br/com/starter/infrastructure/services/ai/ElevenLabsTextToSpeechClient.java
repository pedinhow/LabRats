package br.com.starter.infrastructure.services.ai;

import java.time.Duration;

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
                    ElevenLabsErrorResponseSanitizer.buildHttpErrorMessage(objectMapper, exception, apiKey),
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

}
