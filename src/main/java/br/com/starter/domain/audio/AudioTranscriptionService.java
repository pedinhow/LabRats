package br.com.starter.domain.audio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AudioTranscriptionService {
    public static final long MAX_AUDIO_SIZE_BYTES = 25L * 1024 * 1024;

    private final SpeechToTextGateway speechToTextGateway;

    public AudioTranscriptionService(SpeechToTextGateway speechToTextGateway) {
        this.speechToTextGateway = speechToTextGateway;
    }

    public String transcribe(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de audio nao informado.");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de audio vazio.");
        }

        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo de audio excede o limite de 25 MB."
            );
        }

        byte[] audio = readAudio(file);
        AudioFormat format = detectFormat(audio, file.getContentType());
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de audio nao suportado.");
        }

        return speechToTextGateway.transcribe("audio." + format.extension(), format.contentType(), audio);
    }

    private byte[] readAudio(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo de audio.", exception);
        }
    }

    private AudioFormat detectFormat(byte[] content, String declaredContentType) {
        if (startsWith(content, "ID3".getBytes(StandardCharsets.US_ASCII)) || isMp3Frame(content)) {
            return new AudioFormat("mp3", "audio/mpeg");
        }
        if (matchesAscii(content, 0, "RIFF") && matchesAscii(content, 8, "WAVE")) {
            return new AudioFormat("wav", "audio/wav");
        }
        if (matchesAscii(content, 0, "fLaC")) {
            return new AudioFormat("flac", "audio/flac");
        }
        if (matchesAscii(content, 0, "OggS")) {
            return new AudioFormat("ogg", "audio/ogg");
        }
        if (isAacAdts(content)) {
            return new AudioFormat("aac", "audio/aac");
        }
        if (matchesAscii(content, 0, "FORM")
                && (matchesAscii(content, 8, "AIFF") || matchesAscii(content, 8, "AIFC"))) {
            return new AudioFormat("aiff", "audio/aiff");
        }
        if (startsWith(content, new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 })
                && isAudioContentType(declaredContentType, "audio/webm")) {
            return new AudioFormat("webm", "audio/webm");
        }
        if (matchesAscii(content, 4, "ftyp") && isAudioMp4(content, declaredContentType)) {
            return new AudioFormat("m4a", "audio/mp4");
        }

        return null;
    }

    private boolean isMp3Frame(byte[] content) {
        return content.length >= 2
                && (content[0] & 0xFF) == 0xFF
                && ((content[1] & 0xE0) == 0xE0)
                && !isAacAdts(content);
    }

    private boolean isAacAdts(byte[] content) {
        return content.length >= 2
                && (content[0] & 0xFF) == 0xFF
                && ((content[1] & 0xF6) == 0xF0);
    }

    private boolean isAudioMp4(byte[] content, String declaredContentType) {
        String brand = content.length >= 12
                ? new String(content, 8, 4, StandardCharsets.US_ASCII)
                : "";

        return brand.startsWith("M4A")
                || brand.startsWith("M4B")
                || isAudioContentType(declaredContentType, "audio/mp4", "audio/x-m4a");
    }

    private boolean isAudioContentType(String value, String... acceptedTypes) {
        if (value == null) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        for (String acceptedType : acceptedTypes) {
            if (acceptedType.equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesAscii(byte[] content, int offset, String signature) {
        return startsWith(content, offset, signature.getBytes(StandardCharsets.US_ASCII));
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        return startsWith(content, 0, signature);
    }

    private boolean startsWith(byte[] content, int offset, byte[] signature) {
        if (content.length < offset + signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (content[offset + index] != signature[index]) {
                return false;
            }
        }

        return true;
    }

    private record AudioFormat(String extension, String contentType) {
    }
}
