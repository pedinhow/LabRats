package br.com.starter.infrastructure.config.audio;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class AudioUploadConfiguration {
    private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(25);
    private static final DataSize MAX_REQUEST_SIZE = DataSize.ofMegabytes(26);

    @Bean
    MultipartConfigElement audioMultipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_FILE_SIZE);
        factory.setMaxRequestSize(MAX_REQUEST_SIZE);
        return factory.createMultipartConfig();
    }
}
