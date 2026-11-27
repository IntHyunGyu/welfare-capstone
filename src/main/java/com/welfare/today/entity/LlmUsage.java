package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** LLM 요약 호출 1건의 토큰 사용량. 관리자 페이지의 사용량/추정 비용 집계에 쓴다. */
@Entity
@Table(name = "llm_usage", indexes = @Index(name = "idx_llm_usage_created_at", columnList = "created_at"))
public class LlmUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serv_id", nullable = false, length = 20)
    private String servId;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LlmUsage() {
    }

    public LlmUsage(String servId, String model, long inputTokens, long outputTokens, LocalDateTime createdAt) {
        this.servId = servId;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = createdAt;
    }

    public String getServId() {
        return servId;
    }

    public String getModel() {
        return model;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
