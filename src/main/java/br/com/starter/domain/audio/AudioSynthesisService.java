package br.com.starter.domain.audio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AudioSynthesisService {
    private final TextToSpeechGateway textToSpeechGateway;

    public AudioSynthesisService(TextToSpeechGateway textToSpeechGateway) {
        this.textToSpeechGateway = textToSpeechGateway;
    }

    public byte[] synthesize(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto nao pode ser vazio.");
        }

        return textToSpeechGateway.synthesize(text.trim());
    }
}
