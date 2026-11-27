package com.welfare.today.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String apiKey,
        String model
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
