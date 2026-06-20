package br.com.starter.domain.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class AudioTranscriptionServiceTest {

    @Test
    void validatesAudioAndCallsGatewayWithDetectedFormat() {
        CapturingGateway gateway = new CapturingGateway();
        AudioTranscriptionService service = new AudioTranscriptionService(gateway);
        byte[] audio = new byte[] { 'I', 'D', '3', 1, 2, 3 };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "not-trusted.txt",
                "text/plain",
                audio
        );

        String result = service.transcribe(file);

        assertThat(result).isEqualTo("Texto transcrito.");
        assertThat(gateway.filename).isEqualTo("audio.mp3");
        assertThat(gateway.contentType).isEqualTo("audio/mpeg");
        assertThat(gateway.audio).containsExactly(audio);
    }

    @Test
    void acceptsWaveAudioByFileSignature() {
        CapturingGateway gateway = new CapturingGateway();
        AudioTranscriptionService service = new AudioTranscriptionService(gateway);
        byte[] wave = new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 1
        };

        service.transcribe(new MockMultipartFile("file", "recording.bin", null, wave));

        assertThat(gateway.contentType).isEqualTo("audio/wav");
        assertThat(gateway.filename).isEqualTo("audio.wav");
    }

    @Test
    void rejectsUnsupportedContentEvenWhenFilenameLooksValid() {
        AudioTranscriptionService service = new AudioTranscriptionService((filename, contentType, audio) -> "text");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.mp3",
                "audio/mpeg",
                "not audio".getBytes()
        );

        assertThatThrownBy(() -> service.transcribe(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void rejectsFileAboveLocalLimitBeforeReadingIt() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(AudioTranscriptionService.MAX_AUDIO_SIZE_BYTES + 1);
        AudioTranscriptionService service = new AudioTranscriptionService((filename, contentType, audio) -> "text");

        assertThatThrownBy(() -> service.transcribe(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void rejectsNullAndEmptyFiles() {
        AudioTranscriptionService service = new AudioTranscriptionService((filename, contentType, audio) -> "text");

        assertThatThrownBy(() -> service.transcribe(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> service.transcribe(emptyFile))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static class CapturingGateway implements SpeechToTextGateway {
        private byte[] audio;
        private String contentType;
        private String filename;

        @Override
        public String transcribe(String filename, String contentType, byte[] audio) {
            this.audio = audio;
            this.contentType = contentType;
            this.filename = filename;
            return "Texto transcrito.";
        }
    }
}
