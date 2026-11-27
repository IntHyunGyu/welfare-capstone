package com.welfare.today.service;

import com.welfare.today.config.LlmProperties;
import com.welfare.today.entity.DailyMetric;
import com.welfare.today.entity.LlmUsage;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.DailyMetricRepository;
import com.welfare.today.repository.LlmUsageRepository;
import com.welfare.today.repository.MemberRepository;
import com.welfare.today.repository.WelfareBenefitDetailRepository;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    /** 공공데이터포털 개발계정 일 호출 한도. */
    public static final int API_DAILY_QUOTA = 1_000;
    private static final int DAYS = 7;

    private final DailyMetricRepository metricRepository;
    private final LlmUsageRepository usageRepository;
    private final WelfareBenefitRepository benefitRepository;
    private final WelfareBenefitDetailRepository detailRepository;
    private final MemberRepository memberRepository;
    private final LlmProperties llmProperties;

    public AdminDashboardService(DailyMetricRepository metricRepository, LlmUsageRepository usageRepository,
                                 WelfareBenefitRepository benefitRepository,
                                 WelfareBenefitDetailRepository detailRepository,
                                 MemberRepository memberRepository, LlmProperties llmProperties) {
        this.metricRepository = metricRepository;
        this.usageRepository = usageRepository;
        this.benefitRepository = benefitRepository;
        this.detailRepository = detailRepository;
        this.memberRepository = memberRepository;
        this.llmProperties = llmProperties;
    }

    public record DayStats(LocalDate date, long pageViews, long apiList, long apiDetail,
                           long llmCalls, long inputTokens, long outputTokens, double costUsd) {
        public long apiCalls() {
            return apiList + apiDetail;
        }

        public int apiQuotaPercent() {
            return (int) Math.min(100, Math.round(apiCalls() * 100.0 / API_DAILY_QUOTA));
        }
    }

    public record LlmTotals(long calls, long inputTokens, long outputTokens, double costUsd) {
    }

    public record DataStatus(long benefits, long newBenefits, LocalDateTime lastFetchedAt, long details, long summaries,
                             long members) {
    }

    public record Dashboard(DayStats today, List<DayStats> recentDays, LlmTotals llmTotals, DataStatus data,
                            String model, int apiDailyQuota) {
    }

    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(MetricService.SEOUL);
        LocalDate from = today.minusDays(DAYS - 1);

        Map<LocalDate, Map<String, Long>> metrics = new HashMap<>();
        for (DailyMetric metric : metricRepository.findByMetricDateGreaterThanEqual(from)) {
            metrics.computeIfAbsent(metric.getMetricDate(), d -> new HashMap<>())
                    .put(metric.getMetricKey(), metric.getMetricCount());
        }
        Map<LocalDate, List<LlmUsage>> usageByDay = new HashMap<>();
        for (LlmUsage usage : usageRepository.findByCreatedAtGreaterThanEqual(from.atStartOfDay())) {
            usageByDay.computeIfAbsent(usage.getCreatedAt().toLocalDate(), d -> new ArrayList<>()).add(usage);
        }

        List<DayStats> recentDays = new ArrayList<>();
        for (LocalDate date = today; !date.isBefore(from); date = date.minusDays(1)) {
            recentDays.add(dayStats(date, metrics.getOrDefault(date, Map.of()), usageByDay.getOrDefault(date, List.of())));
        }

        Object[] totals = usageRepository.totals().get(0);
        long totalIn = ((Number) totals[1]).longValue();
        long totalOut = ((Number) totals[2]).longValue();
        LlmTotals llmTotals = new LlmTotals(((Number) totals[0]).longValue(), totalIn, totalOut,
                llmProperties.costUsd(totalIn, totalOut));

        long newBenefits = benefitRepository.countByFirstSeenAtGreaterThanEqual(
                today.minusDays(WelfareBenefit.NEW_DAYS - 1).atStartOfDay());
        DataStatus data = new DataStatus(benefitRepository.count(), newBenefits, benefitRepository.findLastFetchedAt(),
                detailRepository.count(), detailRepository.countByLlmSummaryIsNotNull(), memberRepository.count());

        return new Dashboard(recentDays.get(0), recentDays, llmTotals, data, llmProperties.model(), API_DAILY_QUOTA);
    }

    private DayStats dayStats(LocalDate date, Map<String, Long> metrics, List<LlmUsage> usages) {
        long in = usages.stream().mapToLong(LlmUsage::getInputTokens).sum();
        long out = usages.stream().mapToLong(LlmUsage::getOutputTokens).sum();
        return new DayStats(date,
                metrics.getOrDefault(DailyMetric.PAGE_VIEW, 0L),
                metrics.getOrDefault(DailyMetric.API_LIST, 0L),
                metrics.getOrDefault(DailyMetric.API_DETAIL, 0L),
                usages.size(), in, out, llmProperties.costUsd(in, out));
    }
}
