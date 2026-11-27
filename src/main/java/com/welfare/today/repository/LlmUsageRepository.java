package com.welfare.today.repository;

import com.welfare.today.entity.LlmUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface LlmUsageRepository extends JpaRepository<LlmUsage, Long> {

    List<LlmUsage> findByCreatedAtGreaterThanEqual(LocalDateTime from);

    @Query("select count(u), coalesce(sum(u.inputTokens), 0), coalesce(sum(u.outputTokens), 0) from LlmUsage u")
    List<Object[]> totals();
}
