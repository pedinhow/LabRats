package br.com.starter.infrastructure.services.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

class ElevenLabsTextToSpeechClientTest {
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");
    private static final byte[] AUDIO = new byte[] { 73, 68, 51 };
    private static final String EXPECTED_URL =
            "https://api.elevenlabs.io/v1/text-to-speech/test-voice?output_format=mp3_44100_128";

    @Test
    void sendsTextToElevenLabsAndReturnsAudio() {
        ClientFixture fixture = createClient("test-key", "test-voice");
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("xi-api-key", "test-key"))
                .andExpect(header(HttpHeaders.ACCEPT, "audio/mpeg"))
                .andExpect(jsonPath("$.text").value("Qual e a capital do Brasil?"))
                .andExpect(jsonPath("$.model_id").value("eleven_multilingual_v2"))
                .andRespond(withSuccess(AUDIO, AUDIO_MPEG));

        byte[] result = fixture.client.synthesize("Qual e a capital do Brasil?");

        assertThat(result).containsExactly(AUDIO);
        fixture.server.verify();
    }

    @Test
    void readsConfigurationWithHiddenWhitespace() {
        ClientFixture fixture = createClient(
                new MockEnvironment()
                        .withProperty("ELEVENLABS_API_KEY ", " test-key ")
                        .withProperty("ELEVENLABS_VOICE_ID ", " test-voice ")
        );
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("xi-api-key", "test-key"))
                .andRespond(withSuccess(AUDIO, AUDIO_MPEG));

        byte[] result = fixture.client.synthesize("Texto");

        assertThat(result).containsExactly(AUDIO);
        fixture.server.verify();
    }

    @Test
    void rejectsMissingApiKey() {
        ClientFixture fixture = createClient(" ", "test-voice");

        assertThatThrownBy(() -> fixture.client.synthesize("Texto"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void rejectsMissingVoiceId() {
        ClientFixture fixture = createClient("test-key", "");

        assertThatThrownBy(() -> fixture.client.synthesize("Texto"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handlesElevenLabsPaymentRequiredJsonError() {
        ClientFixture fixture = createClient("test-key", "test-voice");
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": {
                                    "status": "insufficient_credits",
                                    "message": "This request exceeds your quota. xi-api-key: sk_secret_value"
                                  }
                                }
                                """));

        Throwable thrown = catchThrowable(() -> fixture.client.synthesize("Texto"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason())
                .contains("402")
                .contains("insufficient_credits")
                .contains("This request exceeds your quota")
                .doesNotContain("sk_secret_value");
    }

    @Test
    void handlesElevenLabsInvalidJsonError() {
        ClientFixture fixture = createClient("test-key", "test-voice");
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("quota exceeded token=sk_secret_value"));

        Throwable thrown = catchThrowable(() -> fixture.client.synthesize("Texto"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason())
                .contains("402")
                .contains("quota exceeded")
                .doesNotContain("sk_secret_value");
    }

    @Test
    void handlesElevenLabsTimeout() {
        ClientFixture fixture = createClient("test-key", "test-voice");
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> fixture.client.synthesize("Texto"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void rejectsEmptyAudioResponse() {
        ClientFixture fixture = createClient("test-key", "test-voice");
        fixture.server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(new byte[0], AUDIO_MPEG));

        assertThatThrownBy(() -> fixture.client.synthesize("Texto"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    private ClientFixture createClient(String apiKey, String voiceId) {
        return createClient(
                new MockEnvironment()
                        .withProperty("ELEVENLABS_API_KEY", apiKey)
                        .withProperty("ELEVENLABS_VOICE_ID", voiceId)
        );
    }

    private ClientFixture createClient(MockEnvironment environment) {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        ElevenLabsTextToSpeechClient client = new ElevenLabsTextToSpeechClient(
                environment,
                new ObjectMapper(),
                new RestTemplateBuilder(customizer)
        );

        return new ClientFixture(client, customizer.getServer());
    }

    private record ClientFixture(ElevenLabsTextToSpeechClient client, MockRestServiceServer server) {
    }
}
