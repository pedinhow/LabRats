package br.com.starter.infrastructure.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    @Test
    void allowsPatchRequestsFromFrontend() {
        CorsConfigurationSource source = CorsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/quizzes/quiz-id/title");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedMethods()).contains("PATCH");
    }
}
