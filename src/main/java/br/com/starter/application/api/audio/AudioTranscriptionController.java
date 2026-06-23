package br.com.starter.application.api.audio;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.starter.application.api.audio.dto.AudioTranscriptionResponseDTO;
import br.com.starter.domain.audio.AudioTranscriptionService;

@RestController
@RequestMapping("/audio")
public class AudioTranscriptionController {
    private final AudioTranscriptionService audioTranscriptionService;

    public AudioTranscriptionController(AudioTranscriptionService audioTranscriptionService) {
        this.audioTranscriptionService = audioTranscriptionService;
    }

    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AudioTranscriptionResponseDTO> transcribe(
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(new AudioTranscriptionResponseDTO(audioTranscriptionService.transcribe(file)));
    }
}
