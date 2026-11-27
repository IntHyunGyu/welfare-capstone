package com.welfare.today.controller;

import com.welfare.today.repository.WelfareBenefitDetailRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 저장된 LLM 요약을 지워, 다음 상세 조회 때 현재 프롬프트로 다시 만들게 한다 (프롬프트 수정 후 사용).
 * /admin/** 이므로 AdminTokenInterceptor의 X-Admin-Token 검사를 받는다.
 */
@RestController
@RequestMapping("/admin/summaries")
public class SummaryAdminController {

    private final WelfareBenefitDetailRepository detailRepository;

    public SummaryAdminController(WelfareBenefitDetailRepository detailRepository) {
        this.detailRepository = detailRepository;
    }

    @PostMapping("/reset")
    public Map<String, Integer> reset(@RequestParam(required = false) String servId) {
        int cleared = servId == null
                ? detailRepository.clearAllSummaries()
                : detailRepository.clearSummary(servId);
        return Map.of("cleared", cleared);
    }
}
