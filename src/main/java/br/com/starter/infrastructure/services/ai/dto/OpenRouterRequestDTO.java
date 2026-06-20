package br.com.starter.infrastructure.services.ai.dto;

import java.util.List;

public record OpenRouterRequestDTO(String model, List<OpenRouterMessageDTO> messages) {
}
