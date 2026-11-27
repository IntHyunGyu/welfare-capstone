package com.welfare.today.service;

import com.anthropic.errors.AnthropicException;
import com.welfare.today.dto.WelfareDetailResponse;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.entity.WelfareBenefitDetail;
import com.welfare.today.repository.WelfareBenefitDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 상세조회와 LLM 요약은 사용자가 상세 화면을 열 때 온디맨드로 만들고 DB에 캐싱한다 -
 * 같은 servId는 API/LLM을 다시 호출하지 않는다.
 */
@Service
public class WelfareBenefitDetailService {

    private static final Logger log = LoggerFactory.getLogger(WelfareBenefitDetailService.class);

    private final WelfareBenefitDetailRepository detailRepository;
    private final WelfareApiService apiService;
    private final LlmSummaryService summaryService;

    public WelfareBenefitDetailService(WelfareBenefitDetailRepository detailRepository,
                                       WelfareApiService apiService,
                                       LlmSummaryService summaryService) {
        this.detailRepository = detailRepository;
        this.apiService = apiService;
        this.summaryService = summaryService;
    }

    public WelfareBenefitDetail getOrFetch(WelfareBenefit benefit) {
        WelfareBenefitDetail detail = detailRepository.findById(benefit.getServId())
                .orElseGet(() -> fetchAndCache(benefit.getServId()));
        if (detail.getLlmSummary() == null && summaryService.enabled()) {
            return summarizeAndCache(benefit, detail);
        }
        return detail;
    }

    private WelfareBenefitDetail fetchAndCache(String servId) {
        WelfareDetailResponse response = apiService.fetchDetail(servId);
        return detailRepository.save(new WelfareBenefitDetail(
                servId,
                response.sprtTrgtCn(),
                response.slctCritCn(),
                response.alwServCn(),
                response.aplyMtdCn(),
                response.enfcBgngYmd(),
                response.enfcEndYmd(),
                null
        ));
    }

    private WelfareBenefitDetail summarizeAndCache(WelfareBenefit benefit, WelfareBenefitDetail detail) {
        try {
            return summaryService.summarize(benefit, detail)
                    .map(summary -> {
                        detail.updateLlmSummary(summary);
                        return detailRepository.save(detail);
                    })
                    .orElse(detail);
        } catch (AnthropicException e) {
            // 요약 실패는 화면을 막지 않는다 - 원문만 보여주고 다음 조회 때 다시 시도.
            log.warn("LLM 요약 실패 servId={}: {}", benefit.getServId(), e.getMessage());
            return detail;
        }
    }
}
