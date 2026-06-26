package br.com.starter.domain.quiz;

public interface QuizGeneratorClient {
    Quiz generateFromText(String baseText);
    Quiz generateFromPdf(String filename, byte[] pdfBytes, String userMessage);
}
