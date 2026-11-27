package com.welfare.today.service;

import com.anthropic.errors.AnthropicException;
import com.welfare.today.dto.WelfareDetailResponse;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.entity.WelfareBenefitDetail;
import com.welfare.today.repository.WelfareBenefitDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 상세조회는 상세 화면을 열 때, LLM 요약은 화면이 뜬 뒤 스크립트가 따로 요청할 때 온디맨드로 만들고
 * DB에 캐싱한다 - 같은 servId는 API/LLM을 다시 호출하지 않는다.
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
        return detailRepository.findById(benefit.getServId())
                .orElseGet(() -> fetchAndCache(benefit.getServId()));
    }

    public boolean summaryEnabled() {
        return summaryService.enabled();
    }

    /**
     * 저장된 요약이 있으면 그대로, 없으면 LLM으로 만들어 저장한다. 키가 없거나 실패하면 빈 값.
     */
    public Optional<String> getOrCreateSummary(WelfareBenefit benefit) {
        WelfareBenefitDetail detail = getOrFetch(benefit);
        if (detail.getLlmSummary() == null && summaryService.enabled()) {
            detail = summarizeAndCache(benefit, detail);
        }
        return Optional.ofNullable(detail.getLlmSummary());
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
