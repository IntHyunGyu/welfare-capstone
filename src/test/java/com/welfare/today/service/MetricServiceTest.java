package com.welfare.today.service;

import com.welfare.today.entity.DailyMetric;
import com.welfare.today.repository.DailyMetricRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// MetricService는 REQUIRES_NEW로 커밋하므로 테스트 트랜잭션으로 감싸지 않는다.
@DataJpaTest
@Import(MetricService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MetricServiceTest {

    @Autowired
    private MetricService metricService;

    @Autowired
    private DailyMetricRepository repository;

    @Test
    void incrementsAccumulatePerKeyForToday() {
        metricService.increment("test.a");
        metricService.increment("test.a");
        metricService.increment("test.a");
        metricService.increment("test.b");

        LocalDate today = LocalDate.now(MetricService.SEOUL);
        assertThat(count(today, "test.a")).isEqualTo(3);
        assertThat(count(today, "test.b")).isEqualTo(1);
    }

    private long count(LocalDate date, String key) {
        return repository.findByMetricDateGreaterThanEqual(date).stream()
                .filter(metric -> metric.getMetricKey().equals(key))
                .mapToLong(DailyMetric::getMetricCount)
                .sum();
    }
}
