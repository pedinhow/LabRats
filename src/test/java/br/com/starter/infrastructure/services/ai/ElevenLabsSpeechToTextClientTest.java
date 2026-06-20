package br.com.starter.infrastructure.services.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

class ElevenLabsSpeechToTextClientTest {
    private static final String API_KEY = "test-secret-value";
    private static final String EXPECTED_URL = "https://api.elevenlabs.io/v1/speech-to-text";

    @Test
    void sendsMultipartFileModelAndApiKeyAndReturnsText() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("xi-api-key", API_KEY))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getAccept()).contains(MediaType.APPLICATION_JSON);
                    assertThat(request.getHeaders().getContentType()).isNotNull();
                    assertThat(request.getHeaders().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                            .isTrue();

                    String body = ((MockClientHttpRequest) request)
                            .getBodyAsString(StandardCharsets.ISO_8859_1);
                    assertThat(body)
                            .contains("name=\"model_id\"")
                            .contains("scribe_v2")
                            .contains("name=\"file\"")
                            .contains("filename=\"audio.mp3\"")
                            .contains("Content-Type: audio/mpeg");
                })
                .andRespond(withSuccess(
                        """
                                {
                                  "language_code": "pt",
                                  "text": "  Texto transcrito.  ",
                                  "words": []
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        String result = fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1, 2, 3 });

        assertThat(result).isEqualTo("Texto transcrito.");
        fixture.server.verify();
    }

    @Test
    void rejectsMissingApiKeyWithoutCallingElevenLabs() {
        ClientFixture fixture = createClient(" ");

        assertThatThrownBy(() -> fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 }))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        fixture.server.verify();
    }

    @Test
    void sanitizesHttpErrorAndConfiguredApiKey() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": {
                                    "status": "permission_denied",
                                    "message": "Key test-secret-value cannot use speech_to_text."
                                  }
                                }
                                """));

        Throwable thrown = catchThrowable(() ->
                fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 })
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason())
                .contains("403")
                .contains("permission_denied")
                .contains("[REDACTED]")
                .doesNotContain(API_KEY);
    }

    @Test
    void rejectsEmptyResponse() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertBadGatewayWithReason(
                () -> fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 }),
                "Resposta vazia"
        );
    }

    @Test
    void rejectsInvalidJsonResponseWithoutExposingSecret() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("invalid json " + API_KEY, MediaType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() ->
                fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 })
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason())
                .contains("JSON de transcricao invalido")
                .doesNotContain(API_KEY);
    }

    @Test
    void rejectsResponseWithoutTranscriptionText() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("{\"language_code\":\"pt\"}", MediaType.APPLICATION_JSON));

        assertBadGatewayWithReason(
                () -> fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 }),
                "Texto de transcricao ausente"
        );
    }

    @Test
    void handlesTimeout() {
        ClientFixture fixture = createClient(API_KEY);
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() ->
                fixture.client.transcribe("audio.mp3", "audio/mpeg", new byte[] { 1 })
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private void assertBadGatewayWithReason(ThrowingCall call, String reason) {
        Throwable thrown = catchThrowable(call::run);

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).contains(reason);
    }

    private ClientFixture createClient(String apiKey) {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        ElevenLabsSpeechToTextClient client = new ElevenLabsSpeechToTextClient(
                new MockEnvironment().withProperty("ELEVENLABS_API_KEY", apiKey),
                new ObjectMapper(),
                new RestTemplateBuilder(customizer)
        );

        return new ClientFixture(client, customizer.getServer());
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private record ClientFixture(
            ElevenLabsSpeechToTextClient client,
            MockRestServiceServer server
    ) {
    }
}
