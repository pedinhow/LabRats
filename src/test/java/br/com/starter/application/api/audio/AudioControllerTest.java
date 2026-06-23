package br.com.starter.application.api.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import br.com.starter.application.api.audio.dto.SynthesizeAudioRequestDTO;
import br.com.starter.domain.audio.AudioSynthesisService;

class AudioControllerTest {
    private static final byte[] AUDIO = new byte[] { 1, 2, 3 };

    @Test
    void returnsMp3Audio() {
        AudioController controller = new AudioController(new AudioSynthesisService(text -> AUDIO));

        var response = controller.synthesize(new SynthesizeAudioRequestDTO("Qual e a capital do Brasil?"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
        assertThat(response.getBody()).containsExactly(AUDIO);
    }

    @Test
    void rejectsNullRequest() {
        AudioController controller = new AudioController(new AudioSynthesisService(text -> AUDIO));

        assertThatThrownBy(() -> controller.synthesize(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
