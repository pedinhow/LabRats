package br.com.starter.domain.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AudioSynthesisServiceTest {
    private static final byte[] AUDIO = new byte[] { 4, 5, 6 };

    @Test
    void trimsTextBeforeCallingGateway() {
        CapturingGateway gateway = new CapturingGateway();
        AudioSynthesisService service = new AudioSynthesisService(gateway);

        byte[] result = service.synthesize("  Ola mundo  ");

        assertThat(result).containsExactly(AUDIO);
        assertThat(gateway.text).isEqualTo("Ola mundo");
    }

    @Test
    void rejectsNullText() {
        AudioSynthesisService service = new AudioSynthesisService(text -> AUDIO);

        assertThatThrownBy(() -> service.synthesize(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsBlankText() {
        AudioSynthesisService service = new AudioSynthesisService(text -> AUDIO);

        assertThatThrownBy(() -> service.synthesize("   "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static class CapturingGateway implements TextToSpeechGateway {
        private String text;

        @Override
        public byte[] synthesize(String text) {
            this.text = text;
            return AUDIO;
        }
    }
}
