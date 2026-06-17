package br.com.starter.infrastructure.services.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ElevenLabsTextToSpeechRequestDTO(
        String text,
        @JsonProperty("model_id") String modelId
) {
}
