package com.welfare.today.service;

import com.welfare.today.config.LlmProperties;
import com.welfare.today.entity.DailyMetric;
import com.welfare.today.entity.LlmUsage;
import com.welfare.today.repository.DailyMetricRepository;
import com.welfare.today.repository.LlmUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Import({AdminDashboardService.class, AdminDashboardServiceTest.Prices.class})
class AdminDashboardServiceTest {

    @TestConfiguration
    static class Prices {
        @Bean
        LlmProperties llmProperties() {
            return new LlmProperties("", "claude-haiku-5-5", 0.10, 0.50);
        }
    }

    @Autowired
    private AdminDashboardService dashboardService;

    @Autowired
    private LlmUsageRepository usageRepository;

    @Autowired
    private DailyMetricRepository metricRepository;

    @Test
    void groupsTodayAndYesterdayAndEstimatesCost() {
        LocalDate today = LocalDate.now(MetricService.SEOUL);
        LocalDateTime now = LocalDateTime.now(MetricService.SEOUL);
        usageRepository.save(new LlmUsage("A", "claude-haiku-5-5", 1_000_000, 200_000, now));
        usageRepository.save(new LlmUsage("B", "claude-haiku-5-5", 500_000, 100_000, now.minusDays(1)));
        metricRepository.save(new DailyMetric(today, DailyMetric.API_LIST, 25));
        metricRepository.save(new DailyMetric(today, DailyMetric.API_DETAIL, 5));
        metricRepository.save(new DailyMetric(today, DailyMetric.PAGE_VIEW, 42));

        AdminDashboardService.Dashboard dashboard = dashboardService.dashboard();

        AdminDashboardService.DayStats day = dashboard.today();
        assertThat(day.date()).isEqualTo(today);
        assertThat(day.apiCalls()).isEqualTo(30);
        assertThat(day.apiQuotaPercent()).isEqualTo(3);
        assertThat(day.pageViews()).isEqualTo(42);
        assertThat(day.llmCalls()).isEqualTo(1);
        // 100만 입력 × $0.10 + 20만 출력 × $0.50 / 100만 = $0.20
        assertThat(day.costUsd()).isCloseTo(0.20, within(1e-9));

        assertThat(dashboard.recentDays()).hasSize(7);
        assertThat(dashboard.recentDays().get(1).llmCalls()).isEqualTo(1);
        assertThat(dashboard.llmTotals().calls()).isEqualTo(2);
        assertThat(dashboard.llmTotals().costUsd()).isCloseTo(0.30, within(1e-9));
    }
}
