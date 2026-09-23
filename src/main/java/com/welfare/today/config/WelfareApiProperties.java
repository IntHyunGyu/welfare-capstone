package com.welfare.today.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "welfare.api")
public record WelfareApiProperties(
        String serviceKey,
        String baseUrl,
        int pageSize,
        int maxPages
) {
}
