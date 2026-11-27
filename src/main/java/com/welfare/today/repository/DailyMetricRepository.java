package com.welfare.today.repository;

import com.welfare.today.entity.DailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface DailyMetricRepository extends JpaRepository<DailyMetric, Long> {

    @Transactional
    @Modifying
    @Query("update DailyMetric m set m.metricCount = m.metricCount + 1 where m.metricDate = :date and m.metricKey = :key")
    int increment(LocalDate date, String key);

    List<DailyMetric> findByMetricDateGreaterThanEqual(LocalDate from);
}
