package br.com.starter.domain.audio;

public interface TextToSpeechGateway {
    byte[] synthesize(String text);
}
