package br.com.starter.infrastructure.services.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterContentPartDTO(String type, String text, FilePartDTO file) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FilePartDTO(String filename, String file_data) {
    }

    public static OpenRouterContentPartDTO text(String text) {
        return new OpenRouterContentPartDTO("text", text, null);
    }

    public static OpenRouterContentPartDTO pdf(String filename, byte[] pdfBytes) {
        String base64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);
        return new OpenRouterContentPartDTO(
                "file", null,
                new FilePartDTO(filename, "data:application/pdf;base64," + base64)
        );
    }
}