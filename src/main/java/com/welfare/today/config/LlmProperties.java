package com.welfare.today.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param inputPricePerMillion  입력 토큰 100만 개당 달러 (관리자 페이지 추정 비용용, 모델을 바꾸면 같이 바꿀 것)
 * @param outputPricePerMillion 출력 토큰 100만 개당 달러
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String apiKey,
        String model,
        double inputPricePerMillion,
        double outputPricePerMillion
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public double costUsd(long inputTokens, long outputTokens) {
        return (inputTokens * inputPricePerMillion + outputTokens * outputPricePerMillion) / 1_000_000;
    }
}
