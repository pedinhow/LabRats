package br.com.starter.infrastructure.services.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterMessageDTO(String role, String content) {
}
