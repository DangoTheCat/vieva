package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.application.settings.ImportSettings;
import com.example.vieva.application.settings.RagSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds group-1 settings ({@code vieva.rag.*}, {@code vieva.documents.*}, {@code vieva.import.*})
 * onto plain application-layer objects, keeping Spring Boot binding out of the use cases.
 */
@Configuration
public class QuestionBankSettingsConfig {

    @Bean
    @ConfigurationProperties(prefix = "vieva.rag")
    public RagSettings ragSettings() {
        return new RagSettings();
    }

    @Bean
    @ConfigurationProperties(prefix = "vieva.documents")
    public DocumentSettings documentSettings() {
        return new DocumentSettings();
    }

    @Bean
    @ConfigurationProperties(prefix = "vieva.import")
    public ImportSettings importSettings() {
        return new ImportSettings();
    }
}
