package br.com.starter.infrastructure.services.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterResponseDTO(List<ChoiceDTO> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChoiceDTO(OpenRouterMessageDTO message) {
    }
}
