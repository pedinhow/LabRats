package br.com.starter.domain.audio;

public interface SpeechToTextGateway {
    String transcribe(String filename, String contentType, byte[] audio);
}
