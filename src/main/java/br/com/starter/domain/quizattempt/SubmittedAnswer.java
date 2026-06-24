package br.com.starter.domain.quizattempt;

import java.util.UUID;

public record SubmittedAnswer(UUID questionId, UUID alternativeId) {
}
