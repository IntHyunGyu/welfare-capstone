package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/** 날짜(한국 시간)별 카운터 — 공공데이터 API 호출 수, 페이지 조회 수 등. */
@Entity
@Table(name = "daily_metric",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_metric", columnNames = {"metric_date", "metric_key"}))
public class DailyMetric {

    public static final String API_LIST = "api.list";
    public static final String API_DETAIL = "api.detail";
    public static final String PAGE_VIEW = "page.view";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "metric_key", nullable = false, length = 30)
    private String metricKey;

    @Column(name = "metric_count", nullable = false)
    private long metricCount;

    protected DailyMetric() {
    }

    public DailyMetric(LocalDate metricDate, String metricKey, long metricCount) {
        this.metricDate = metricDate;
        this.metricKey = metricKey;
        this.metricCount = metricCount;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public long getMetricCount() {
        return metricCount;
    }
}
