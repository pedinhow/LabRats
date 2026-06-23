package br.com.starter.infrastructure.services.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

final class ElevenLabsErrorResponseSanitizer {
    private static final int MAX_EXTERNAL_ERROR_LENGTH = 300;

    private ElevenLabsErrorResponseSanitizer() {
    }

    static String buildHttpErrorMessage(
            ObjectMapper objectMapper,
            HttpStatusCodeException exception,
            String... sensitiveValues
    ) {
        String message = "Erro HTTP ao chamar a ElevenLabs: " + exception.getStatusCode().value() + ".";
        String externalMessage = extractExternalErrorMessage(
                objectMapper,
                exception.getResponseBodyAsString(),
                sensitiveValues
        );

        return externalMessage == null
                ? message
                : message + " Detalhe: " + externalMessage;
    }

    private static String extractExternalErrorMessage(
            ObjectMapper objectMapper,
            String responseBody,
            String... sensitiveValues
    ) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return truncate(sanitize(firstExternalMessage(root), sensitiveValues));
        } catch (JsonProcessingException exception) {
            return truncate(sanitize(responseBody, sensitiveValues));
        }
    }

    private static String firstExternalMessage(JsonNode root) {
        Set<String> uniqueParts = new LinkedHashSet<>();
        List<String> parts = new ArrayList<>();
        collectText(root, parts, "status");
        collectText(root, parts, "code");
        collectText(root, parts, "message");
        collectText(root, parts, "detail");
        collectText(root, parts, "error");
        uniqueParts.addAll(parts);

        if (uniqueParts.isEmpty()) {
            return root == null ? null : root.toString();
        }

        return String.join(" - ", uniqueParts);
    }

    private static void collectText(JsonNode node, List<String> parts, String fieldName) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isValueNode()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    parts.add(text);
                }
            }

            node.fields().forEachRemaining(entry -> collectText(entry.getValue(), parts, fieldName));
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, parts, fieldName));
        } else if ("detail".equals(fieldName) && node.isValueNode()) {
            String text = node.asText();
            if (StringUtils.hasText(text)) {
                parts.add(text);
            }
        }
    }

    private static String sanitize(String value, String... sensitiveValues) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String sanitized = value
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer [REDACTED]")
                .replaceAll("(?i)xi-api-key\\s*[:=]\\s*[^\\s,;}\"]+", "xi-api-key: [REDACTED]")
                .replaceAll("(?i)(api[_ -]?key|token|authorization)\\s*[:=]\\s*[^\\s,;}\"]+", "$1: [REDACTED]")
                .replaceAll("sk_[A-Za-z0-9_\\-]+", "[REDACTED]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        for (String sensitiveValue : sensitiveValues) {
            if (StringUtils.hasText(sensitiveValue)) {
                sanitized = sanitized.replaceAll(Pattern.quote(sensitiveValue), "[REDACTED]");
            }
        }

        return sanitized;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_EXTERNAL_ERROR_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_EXTERNAL_ERROR_LENGTH) + "...";
    }
}
