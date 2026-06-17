package br.com.starter.application.api.audio;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.starter.application.api.audio.dto.SynthesizeAudioRequestDTO;
import br.com.starter.domain.audio.AudioSynthesisService;

@RestController
@RequestMapping("/audio")
public class AudioController {
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");

    private final AudioSynthesisService audioSynthesisService;

    public AudioController(AudioSynthesisService audioSynthesisService) {
        this.audioSynthesisService = audioSynthesisService;
    }

    @PostMapping(value = "/synthesize", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(@RequestBody(required = false) SynthesizeAudioRequestDTO request) {
        byte[] audio = audioSynthesisService.synthesize(request == null ? null : request.text());

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(audio);
    }
}
