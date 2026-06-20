package br.com.starter.infrastructure.services.ai;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import br.com.starter.domain.audio.SpeechToTextGateway;
import br.com.starter.infrastructure.services.ai.dto.ElevenLabsSpeechToTextResponseDTO;

@Service
public class ElevenLabsSpeechToTextClient implements SpeechToTextGateway {
    private static final String API_KEY_ENV = "ELEVENLABS_API_KEY";
    private static final String API_KEY_HEADER = "xi-api-key";
    private static final String API_KEY_PROPERTY = "elevenlabs.api-key";
    private static final String API_URL = "https://api.elevenlabs.io/v1/speech-to-text";
    private static final String MODEL = "scribe_v2";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ElevenLabsSpeechToTextClient(
            Environment environment,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.apiKey = readConfiguration(environment, API_KEY_PROPERTY, API_KEY_ENV);
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(120))
                .build();
    }

    @Override
    public String transcribe(String filename, String contentType, byte[] audio) {
        validateConfiguration();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    API_URL,
                    new HttpEntity<>(buildMultipartBody(filename, contentType, audio), buildHeaders()),
                    String.class
            );

            return parseTranscription(response.getBody());
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
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave da ElevenLabs nao configurada.");
        }
    }

    private MultiValueMap<String, Object> buildMultipartBody(
            String filename,
            String contentType,
            byte[] audio
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model_id", MODEL);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        body.add("file", new HttpEntity<>(new NamedByteArrayResource(audio, filename), fileHeaders));
        return body;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String parseTranscription(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta vazia da ElevenLabs.");
        }

        try {
            ElevenLabsSpeechToTextResponseDTO response = objectMapper.readValue(
                    responseBody,
                    ElevenLabsSpeechToTextResponseDTO.class
            );
            if (response == null || !StringUtils.hasText(response.text())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Texto de transcricao ausente na resposta da ElevenLabs."
                );
            }

            return response.text().trim();
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A ElevenLabs retornou um JSON de transcricao invalido.",
                    exception
            );
        }
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

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
