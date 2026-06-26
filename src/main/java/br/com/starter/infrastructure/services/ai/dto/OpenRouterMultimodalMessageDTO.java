package br.com.starter.infrastructure.services.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterMultimodalMessageDTO(String role, List<OpenRouterContentPartDTO> content) {
}